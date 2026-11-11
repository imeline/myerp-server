package erp.order.dto.response;

import erp.order.dto.internal.OrderDetailRow;
import erp.order.dto.internal.OrderItemDetailRow;
import erp.order.enums.OrderStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

@Builder
public record OrderDetailResponse(
        String code,
        String customer,
        LocalDate orderDate,
        OrderStatus status,
        String employeeName,
        Integer totalItemCount,
        Integer totalQuantity,
        List<OrderItemDetailResponse> items,
        Integer totalAmount,
        LocalDateTime canceledAt,
        String canceledByName
) {
    public static OrderDetailResponse of(OrderDetailRow header, List<OrderItemDetailRow> itemRows) {
        List<OrderItemDetailResponse> items = itemRows.stream()
                .map(OrderItemDetailResponse::from)
                .toList();

        return OrderDetailResponse.builder()
                .code(header.code())
                .customer(header.customer())
                .orderDate(header.orderDate())
                .status(header.status())
                .employeeName(header.employeeName())
                .totalItemCount(header.totalItemCount())
                .totalQuantity(header.totalQuantity())
                .items(items)
                .totalAmount(header.totalAmount())
                .canceledAt(header.canceledAt())
                .canceledByName(header.canceledByName())
                .build();
    }
}
