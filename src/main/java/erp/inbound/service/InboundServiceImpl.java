package erp.inbound.service;

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
import erp.inbound.domain.Inbound;
import erp.inbound.dto.internal.InboundCancelItemRow;
import erp.inbound.dto.internal.InboundFindAllRow;
import erp.inbound.dto.request.InboundFindAllRequest;
import erp.inbound.dto.request.InboundSaveRequest;
import erp.inbound.dto.response.InboundFindAllResponse;
import erp.inbound.mapper.InboundMapper;
import erp.inbound.validation.InboundValidator;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import erp.purchase.dto.internal.PurchaseItemQuantityRow;
import erp.purchase.service.PurchaseService;
import erp.purchase.validation.PurchaseValidator;
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
public class InboundServiceImpl implements InboundService {

    private final InboundMapper inboundMapper;
    private final StockService stockService;
    private final PurchaseService purchaseService;
    private final InboundValidator inboundValidator;
    private final EmployeeService employeeService;
    private final PurchaseValidator purchaseValidator;

    @Auditable(type = LogType.WORK,
            messageEl = "'입고 등록: purchaseId=' + #args[0].purchaseId() + ', date=' + #args[0].inboundDate()")
    @Override
    @Transactional
    public long saveInbound(InboundSaveRequest request, long tenantId, String accountUuid) {
        LocalDate inboundDate = request.inboundDate();
        long purchaseId = request.purchaseId();
        long employeeId = employeeService.findActiveEmployeeIdByAccountUuid(accountUuid, tenantId); // 담당자 = 로그인한 본인 (재직 검증 포함)

        purchaseValidator.validPurchaseIdIfPresent(purchaseId, tenantId);

        // 1) 발주 품목 수량 조회
        List<PurchaseItemQuantityRow> itemRows =
                purchaseService.findAllPurchaseItemQuantityRow(purchaseId, tenantId);

        // 2) 입고 저장(코드 유니크 충돌 시 재시도)
        // -> 동시성 문제로 인해, 코드 중복을 미리 검사하지 말고 save 시 에러 처리로 대체
        long newInboundId = 0L;
        int year = inboundDate.getYear();

        for (int attempt = 0; attempt < DEFAULT_MAX_TRY; attempt++) {
            String inboundCode = "IB-" + year + "-" + Codes.randomToken(8);

            try {
                long inboundId = inboundMapper.nextId();

                Inbound inbound = Inbound.register(
                        inboundId,
                        inboundCode,
                        inboundDate,
                        employeeId,
                        purchaseId,
                        tenantId
                );

                int affectedRowCount = inboundMapper.save(tenantId, inbound);
                requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_INBOUND_FAIL);

                newInboundId = inboundId;
                break; // 저장 성공 → 루프 탈출

            } catch (DuplicateKeyException e) {
                if (attempt == DEFAULT_MAX_TRY - 1) {
                    throw new GlobalException(ErrorStatus.CREATE_INBOUND_FAIL);
                }
            }
        }

        // 3) 재고 on_hand 증가
        for (PurchaseItemQuantityRow row : itemRows) {
            stockService.increaseOnHand(
                    row.itemId(), row.quantity(), tenantId);
        }

        // 4) 발주 상태 SHIPPED로 변경
        purchaseService.updateStatusToShippedIfConfirmed(purchaseId, tenantId);

        // 5) 성공: inbound_id 반환
        return newInboundId;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InboundFindAllResponse> findAllInbound(
            InboundFindAllRequest request,
            long tenantId
    ) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);

        DatePeriod period = request.period();
        DateRange dateRange = Periods.resolve(period, LocalDate.now());
        LocalDate startDate = dateRange.startDate();
        LocalDate endDate = dateRange.endDate();

        String code = Strings.normalizeOrNull(request.code());

        List<InboundFindAllRow> rows = inboundMapper.findAllInboundFindAllRow(
                tenantId,
                startDate,
                endDate,
                code,
                request.status(),
                pageParam.offset(),
                pageParam.size()
        );

        List<InboundFindAllResponse> responses = rows.stream()
                .map(InboundFindAllResponse::from)
                .toList();

        long total = inboundMapper.countByPeriodAndCode(
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
            messageEl = "'입고 취소: inboundId=' + #args[0]")
    @Override
    @Transactional
    public void cancelInbound(long inboundId, long tenantId, String cancelerUuid) {
        inboundValidator.validInboundActive(inboundId, tenantId);

        List<InboundCancelItemRow> rows = inboundMapper.findAllCancelItemRowById(
                tenantId,
                inboundId
        );
        if (rows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_INBOUND);
        }
        long purchaseId = rows.get(0).purchaseId();

        // 3) 현재 재고 롤백 (입고 수량만큼 on_hand 감소)
        for (InboundCancelItemRow row : rows) {
            stockService.decreaseOnHand(
                    row.itemId(),
                    row.quantity(),
                    tenantId
            );
        }

        // 4) 입고 상태를 CANCELED로 변경
        int affected = inboundMapper.updateStatusToCanceledIfActive(tenantId, inboundId, cancelerUuid);
        requireOneRowAffected(affected, ErrorStatus.CANCEL_INBOUND_FAIL);

        // 5) 발주 상태를 CONFIRMED로 되돌림 (SHIPPED → CONFIRMED)
        // 실패 사유는 PurchaseService에서 처리
        purchaseService.updateStatusToConfirmedIfShipped(purchaseId, tenantId);
    }
}
