package erp.outbound.service;

import erp.employee.service.EmployeeService;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.Codes;
import erp.global.util.PageParam;
import erp.global.util.Strings;
import erp.global.util.time.DatePeriod;
import erp.global.util.time.DateRange;
import erp.global.util.time.Periods;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import erp.order.dto.internal.OrderItemQuantityRow;
import erp.order.service.OrderService;
import erp.order.validation.OrderValidator;
import erp.outbound.domain.Outbound;
import erp.outbound.dto.internal.OutboundCancelItemRow;
import erp.outbound.dto.internal.OutboundFindRow;
import erp.outbound.dto.request.OutboundFindAllRequest;
import erp.outbound.dto.request.OutboundSaveRequest;
import erp.outbound.dto.response.OutboundFindAllResponse;
import erp.outbound.enums.OutboundStatus;
import erp.outbound.mapper.OutboundMapper;
import erp.outbound.validation.OutboundValidator;
import erp.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static erp.global.util.Codes.DEFAULT_MAX_TRY;
import static erp.global.util.RowCountGuards.requireOneRowAffected;

@Service
@RequiredArgsConstructor
public class OutboundServiceImpl implements OutboundService {

    private final OutboundMapper outboundMapper;
    private final OrderService orderService;
    private final StockService stockService;

    private final OutboundValidator outboundValidator;
    private final EmployeeService employeeService;
    private final OrderValidator orderValidator;

    @Auditable(type = LogType.WORK,
            messageEl = "'출고 등록: orderId=' + #args[0].orderId() + ', date=' + #args[0].outboundDate()")
    @Override
    @Transactional
    public long saveOutbound(OutboundSaveRequest request, long tenantId, String accountUuid) {
        LocalDate outboundDate = request.outboundDate();
        long orderId = request.orderId();
        long employeeId = employeeService.findActiveEmployeeIdByAccountUuid(accountUuid, tenantId); // 담당자 = 로그인한 본인 (재직 검증 포함)

        orderValidator.validOrderIdIfPresent(orderId, tenantId);

        // 1) 주문 품목 수량 조회
        List<OrderItemQuantityRow> itemRows =
                orderService.findAllOrderItemQuantityRow(orderId, tenantId);

        // 2) 재고 체크 및 차감
        for (OrderItemQuantityRow row : itemRows) {
            long itemId = row.itemId();
            int quantity = row.quantity();

            // 현재 재고(on_hand) 감소 — 부족하면 예외
            stockService.decreaseOnHand(itemId, quantity, tenantId);

            // 예약 재고(allocated) 감소 — 부족하면 예외
            stockService.decreaseAllocated(tenantId, itemId, quantity);
        }

        // 3) 출고 저장 (코드 유니크 충돌 시 재시도)
        int year = outboundDate.getYear();
        for (int attempt = 0; attempt < DEFAULT_MAX_TRY; attempt++) {
            String outboundCode = "OB-" + year + "-" + Codes.randomToken(8);
            try {
                long newOutboundId = outboundMapper.nextId();

                Outbound outbound = Outbound.builder()
                        .outboundId(newOutboundId)
                        .code(outboundCode)
                        .outboundDate(outboundDate)
                        .status(OutboundStatus.ACTIVE)
                        .employeeId(employeeId)
                        .orderId(orderId)
                        .companyId(tenantId)
                        .build();

                int affectedRowCount = outboundMapper.save(tenantId, outbound);
                requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_OUTBOUND_FAIL);

                // 5) 주문 상태 SHIPPED로 변경 (CONFIRMED일 때만)
                orderService.updateStatusToShippedIfConfirmed(orderId, tenantId);

                return newOutboundId;

            } catch (DuplicateKeyException e) {
                if (attempt == DEFAULT_MAX_TRY - 1) {
                    throw new GlobalException(ErrorStatus.CREATE_OUTBOUND_FAIL);
                }
                // 재시도
            }
        }

        throw new GlobalException(ErrorStatus.CREATE_OUTBOUND_FAIL);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OutboundFindAllResponse> findAllOutbound(
            OutboundFindAllRequest request,
            long tenantId
    ) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);

        DatePeriod period = request.period();
        DateRange range = Periods.resolve(period, LocalDate.now());
        LocalDate startDate = range.startDate();
        LocalDate endDate = range.endDate();

        String code = Strings.normalizeOrNull(request.code());

        List<OutboundFindRow> rows = outboundMapper.findAllOutboundFindRow(
                tenantId,
                startDate,
                endDate,
                code,
                request.status(),
                pageParam.offset(),
                pageParam.size()
        );

        List<OutboundFindAllResponse> responses = rows.stream()
                .map(OutboundFindAllResponse::from)
                .toList();

        long total = outboundMapper.countByPeriodAndCode(
                tenantId,
                startDate,
                endDate,
                code,
                request.status()
        );

        return PageResponse.of(
                responses,
                pageParam.page(),
                total,
                pageParam.size()
        );
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'출고 취소: outboundId=' + #args[0]")
    @Override
    @Transactional
    public void cancelOutbound(long outboundId, long tenantId, String cancelerUuid) {
        // 1) 출고 존재 + ACTIVE 검증
        outboundValidator.validOutboundIdIfPresent(outboundId, tenantId);

        // 2) 주문 품목 수량 목록 조회
        List<OutboundCancelItemRow> rows =
                outboundMapper.findAllCancelItemRowById(tenantId, outboundId);
        if (rows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_OUTBOUND);
        }
        long orderId = rows.get(0).orderId();

        // 3) 재고 롤백
        //   - 현재재고(on_hand)  : + 수량
        //   - 예약재고(allocated): + 수량 (on_hand 충분 조건을 함께 보장하려면 on_hand 증가 후 호출)
        for (OutboundCancelItemRow row : rows) {
            // on_hand 먼저 복구
            stockService.increaseOnHand(row.itemId(), row.quantity(), tenantId);
            // 예약재고 복구
            stockService.increaseAllocatedIfEnoughAvailable(tenantId, row.itemId(), row.quantity());
        }

        // 4) 출고 상태를 CANCELED로 변경 (ACTIVE일 때만)
        int affectedRowCount = outboundMapper.updateStatusToCanceledIfActive(tenantId, outboundId, cancelerUuid);
        requireOneRowAffected(affectedRowCount, ErrorStatus.UPDATE_OUTBOUND_STAUS_FAIL);

        // 5) 주문 상태를 SHIPPED → CONFIRMED 로 되돌림
        orderService.updateStatusToConfirmedIfShipped(orderId, tenantId);
    }


}
