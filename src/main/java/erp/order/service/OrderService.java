package erp.order.service;

import erp.global.response.PageResponse;
import erp.order.dto.internal.OrderItemQuantityRow;
import erp.order.dto.request.OrderFindAllRequest;
import erp.order.dto.request.OrderSaveRequest;
import erp.order.dto.response.OrderCodeAndCustomerResponse;
import erp.order.dto.response.OrderDetailResponse;
import erp.order.dto.response.OrderFindAllResponse;
import erp.order.dto.response.OrderItemsSummaryResponse;
import erp.order.enums.OrderStatus;

import java.util.List;

public interface OrderService {
    long saveOrderAndOrderItems(OrderSaveRequest request, long tenantId, String accountUuid);

    PageResponse<OrderFindAllResponse> findAllOrder(
            OrderFindAllRequest request, long tenantId);

    OrderItemsSummaryResponse findOrderItemsSummary(
            long orderId, long tenantId);

    List<OrderCodeAndCustomerResponse> findAllOrderCodeAndCustomer(
            long tenantId);

    OrderDetailResponse findOrderDetail(long orderId, long tenantId);

    void cancelOrder(long orderId, long tenantId, String cancelerUuid);

    OrderStatus findStatusById(long orderId, long tenantId);

    List<OrderItemQuantityRow> findAllOrderItemQuantityRow(long orderId, long tenantId);

    void updateStatusToShippedIfConfirmed(long orderId, long tenantId);

    void updateStatusToConfirmedIfShipped(long orderId, long tenantId);
}
