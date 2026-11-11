package erp.order.service;

import erp.employee.validation.EmployeeValidator;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.*;
import erp.global.util.time.DatePeriod;
import erp.global.util.time.DateRange;
import erp.global.util.time.Periods;
import erp.item.dto.internal.ItemPriceRow;
import erp.item.service.ItemService;
import erp.item.validation.ItemValidator;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import erp.order.domain.Order;
import erp.order.domain.OrderItem;
import erp.order.dto.internal.*;
import erp.order.dto.request.OrderFindAllRequest;
import erp.order.dto.request.OrderItemSaveRequest;
import erp.order.dto.request.OrderSaveRequest;
import erp.order.dto.response.*;
import erp.order.enums.OrderStatus;
import erp.order.mapper.OrderItemMapper;
import erp.order.mapper.OrderMapper;
import erp.order.validation.OrderValidator;
import erp.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static erp.global.util.Codes.DEFAULT_MAX_TRY;
import static erp.global.util.RowCountGuards.requireOneRowAffected;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    private final ItemService itemService;
    private final StockService stockService;

    private final OrderValidator orderValidator;
    private final EmployeeValidator employeeValidator;
    private final ItemValidator itemValidator;

    @Auditable(type = LogType.WORK,
            messageEl = "'주문 등록: customer=' + #args[0].customer() + ', items=' + #args[0].items().size()")
    @Override
    @Transactional
    public long saveOrderAndOrderItems(OrderSaveRequest request, long tenantId) {
        String customer = Strings.normalizeOrNull(request.customer());
        LocalDate orderDate = request.orderDate();
        long employeeId = request.employeeId();
        List<OrderItemSaveRequest> requestItems = request.items();

        // 1) 중복 금지
        orderValidator.validItemIdsUniqueInRequest(requestItems);

        // 2) 존재성 검증
        employeeValidator.validEmployeeIdIfPresent(employeeId, tenantId);
        List<Long> itemIds = requestItems.stream().map(OrderItemSaveRequest::itemId).toList();
        itemValidator.validItemIdsIfPresent(itemIds, tenantId);

        // 3) 단가 일괄 조회
        List<ItemPriceRow> priceRows = itemService.findAllItemPriceByIds(itemIds, tenantId);
        Map<Long, Integer> priceMap = priceRows.stream()
                .collect(Collectors.toMap(ItemPriceRow::itemId, ItemPriceRow::price));

        // 4) 재고 규칙: 가용재고(onHand - allocated) >= qty 일 때만 allocated += qty
        for (OrderItemSaveRequest it : requestItems) {
            stockService.increaseAllocatedIfEnoughAvailable(tenantId, it.itemId(), it.quantity()); // 실패 시 내부에서 예외
        }

        // 5) 합계 계산
        Totals totals = TotalCalculators.computeTotals(
                requestItems,
                OrderItemSaveRequest::quantity,
                OrderItemSaveRequest::itemId,
                priceMap
        );

        // 6) 주문 코드 생성 & 저장(유니크 충돌 시 재시도)
        int year = orderDate.getYear();
        for (int attempt = 0; attempt < DEFAULT_MAX_TRY; attempt++) {
            String orderCode = "SO-" + year + "-" + Codes.randomToken(8);
            try {
                long newOrderId = saveOrder(
                        orderCode, customer, orderDate,
                        totals.totalQuantity(), totals.totalAmount(),
                        employeeId, tenantId
                );

                saveOrderItems(newOrderId, requestItems, tenantId);
                return newOrderId;

            } catch (DuplicateKeyException e) {
                if (attempt == DEFAULT_MAX_TRY - 1)
                    throw new GlobalException(ErrorStatus.CREATE_ORDER_FAIL);
            }
        }
        throw new GlobalException(ErrorStatus.CREATE_ORDER_FAIL);
    }

    /**
     * 주문 헤더 저장
     */
    private long saveOrder(String orderCode,
                           String customer,
                           LocalDate orderDate,
                           int totalQuantity,
                           int totalAmount,
                           long employeeId,
                           long tenantId) {
        long newOrderId = orderMapper.nextId();

        Order order = Order.register(
                newOrderId,
                orderCode,
                customer,
                orderDate,
                totalQuantity,
                totalAmount,
                employeeId,
                tenantId
        );

        int affectedRowCount = orderMapper.save(tenantId, order);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_ORDER_FAIL);
        return newOrderId;
    }

    /**
     * 주문 품목 배치 저장
     */
    private void saveOrderItems(long orderId,
                                List<OrderItemSaveRequest> requestItems,
                                long tenantId) {
        for (OrderItemSaveRequest it : requestItems) {
            long newOrderItemId = orderItemMapper.nextId();

            OrderItem row = OrderItem.register(
                    newOrderItemId,
                    it.quantity(),
                    orderId,
                    it.itemId(),
                    tenantId
            );

            int affectedRowCount = orderItemMapper.save(tenantId, row);
            requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_ORDER_ITEM_FAIL);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderFindAllResponse> findAllOrder(OrderFindAllRequest request, long tenantId) {

        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);

        DatePeriod period = request.period();
        DateRange range = Periods.resolve(period, LocalDate.now());
        LocalDate startDate = range.startDate();
        LocalDate endDate = range.endDate();
        String code = Strings.normalizeOrNull(request.code());
        OrderStatus status = request.status();

        List<OrderFindRow> rows = orderMapper.findAllOrderFindRow(
                tenantId,
                startDate,
                endDate,
                code,
                status,
                pageParam.offset(),
                pageParam.size()
        );

        List<OrderFindAllResponse> responses = rows.stream()
                .map(OrderFindAllResponse::from)
                .toList();

        long total = orderMapper.countByPeriodAndCodeAndStatus(
                tenantId,
                startDate,
                endDate,
                code,
                status
        );

        return PageResponse.of(
                responses,
                pageParam.page(),
                total,
                pageParam.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItemsSummaryResponse findOrderItemsSummary(long orderId, long tenantId) {
        orderValidator.validOrderIdIfPresent(orderId, tenantId);

        List<OrderItemStockFindRow> rows =
                orderItemMapper.findAllOrderItemStockFindRow(tenantId, orderId);
        if (rows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_ORDER_ITEM);
        }

        List<OrderItemStockFindResponse> items = rows.stream()
                .map(OrderItemStockFindResponse::from)
                .toList();

        return OrderItemsSummaryResponse.of(items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderCodeAndCustomerResponse> findAllOrderCodeAndCustomer(long tenantId) {
        List<OrderCodeAndCustomerRow> rows = orderMapper.findAllCodeAndCustomer(tenantId);
        return rows.stream()
                .map(OrderCodeAndCustomerResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse findOrderDetail(long orderId, long tenantId) {
        orderValidator.validOrderIdIfPresent(orderId, tenantId);

        OrderDetailRow header = orderMapper.findOrderDetailRow(tenantId, orderId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ORDER));

        List<OrderItemDetailRow> itemRows =
                orderItemMapper.findAllOrderItemDetailRow(tenantId, orderId);
        if (itemRows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_ORDER_ITEM);
        }
        return OrderDetailResponse.of(header, itemRows);
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'주문 취소: orderId=' + #args[0]")
    @Override
    @Transactional
    public void cancelOrder(long orderId, long tenantId) {
        orderValidator.validOrderIdIfPresent(orderId, tenantId);

        // 1) 상태 변경: CONFIRMED → CANCELED
        int affectedRowCount = orderMapper.updateStatusToIfConfirmed(
                tenantId, orderId, OrderStatus.CANCELED
        );
        if (affectedRowCount != 1) {
            // 2) 실패 사유 판정
            OrderStatus status = findStatusById(orderId, tenantId);

            switch (status) {
                case SHIPPED ->
                        throw new GlobalException(ErrorStatus.CANNOT_CANCEL_SHIPPED_ORDER);
                case CANCELED ->
                        throw new GlobalException(ErrorStatus.ALREADY_CANCELED_ORDER);
                default ->
                        throw new GlobalException(ErrorStatus.CANCEL_ORDER_FAIL);
            }
        }

        // 3) 예약재고 롤백: 주문 품목 목록 조회 → allocated -= quantity
        List<OrderItemQuantityRow> rows = findAllOrderItemQuantityRow(orderId, tenantId);
        for (OrderItemQuantityRow row : rows) {
            stockService.decreaseAllocated(tenantId, row.itemId(), row.quantity());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatus findStatusById(long orderId, long tenantId) {
        return orderMapper.findStatusById(tenantId, orderId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ORDER));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemQuantityRow> findAllOrderItemQuantityRow(long orderId, long tenantId) {
        List<OrderItemQuantityRow> rows = orderItemMapper.findAllOrderItemQuantityRow(tenantId, orderId);
        if (rows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_ORDER_ITEM);
        }
        return rows;
    }

    @Override
    @Transactional
    public void updateStatusToShippedIfConfirmed(long orderId, long tenantId) {
        int affected = orderMapper.updateStatusToIfConfirmed(tenantId, orderId, OrderStatus.SHIPPED);
        if (affected == 1) {
            return;
        }
        // 실패 사유 판정
        OrderStatus status = orderMapper.findStatusById(tenantId, orderId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ORDER));
        switch (status) {
            case CANCELED ->
                    throw new GlobalException(ErrorStatus.CANNOT_SHIP_CANCELED_ORDER);
            case SHIPPED ->
                    throw new GlobalException(ErrorStatus.ALREADY_SHIPPED_ORDER);
        }
        throw new GlobalException(ErrorStatus.UPDATE_ORDER_STATUS_FAIL);
    }

    @Override
    @Transactional
    public void updateStatusToConfirmedIfShipped(long orderId, long tenantId) {
        int affectedRowCount = orderMapper.updateStatusToConfirmedIfShipped(tenantId, orderId);
        if (affectedRowCount == 1) {
            return; // 성공
        }

        // 실패 사유 판정
        OrderStatus status = orderMapper.findStatusById(tenantId, orderId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ORDER));

        switch (status) {
            case CANCELED ->
                    throw new GlobalException(ErrorStatus.CANNOT_REVERT_CANCELED_ORDER);
            case CONFIRMED ->
                    throw new GlobalException(ErrorStatus.ALREADY_CONFIRMED_ORDER);
            // SHIPPED가 아니면서 업데이트가 안 된 케이스
            default ->
                    throw new GlobalException(ErrorStatus.UPDATE_ORDER_STATUS_FAIL);
        }
    }
}
