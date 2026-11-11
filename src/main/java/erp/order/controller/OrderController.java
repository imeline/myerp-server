package erp.order.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import erp.order.dto.request.OrderFindAllRequest;
import erp.order.dto.request.OrderSaveRequest;
import erp.order.dto.response.OrderCodeAndCustomerResponse;
import erp.order.dto.response.OrderDetailResponse;
import erp.order.dto.response.OrderFindAllResponse;
import erp.order.dto.response.OrderItemsSummaryResponse;
import erp.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/order")
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 등록 (주문 + 주문아이템 저장)
     */
    @PostMapping
    public ApiResponse<Long> saveOrderAndItems(@Valid @RequestBody OrderSaveRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                orderService.saveOrderAndOrderItems(request, tenantId));
    }

    /**
     * 주문 목록 조회 (기간/상태/코드 검색 + 페이징, 취소 제외)
     */
    @GetMapping
    public ApiResponse<PageResponse<OrderFindAllResponse>> findAllOrders(
            @Valid @ParameterObject @ModelAttribute OrderFindAllRequest request
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                orderService.findAllOrder(request, tenantId));
    }

    /**
     * 특정 주문의 주문아이템 목록 요약 조회(재고 포함 합계)
     */
    @GetMapping("/{orderId}/items/summary")
    public ApiResponse<OrderItemsSummaryResponse> findOrderItemsSummary(
            @PathVariable long orderId
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                orderService.findOrderItemsSummary(orderId, tenantId));
    }

    /**
     * 주문번호·고객사 목록 (CONFIRMED만)
     */
    @GetMapping("/code-customers")
    public ApiResponse<List<OrderCodeAndCustomerResponse>> findAllOrderCodeAndCustomer() {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                orderService.findAllOrderCodeAndCustomer(tenantId));
    }

    /**
     * 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> findOrderDetail(@PathVariable long orderId) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                orderService.findOrderDetail(orderId, tenantId));
    }

    /**
     * 주문 취소 — status를 CANCELED로 변경. SHIPPED/이미 CANCELED면 불가
     * 성공 시 예약재고(allocated) 롤백
     */
    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> cancelOrder(@PathVariable long orderId) {
        long tenantId = TenantContext.get();
        orderService.cancelOrder(orderId, tenantId);
        return ApiResponse.onSuccess(null);
    }
}
