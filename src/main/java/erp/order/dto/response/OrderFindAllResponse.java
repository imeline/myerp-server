package erp.order.dto.response;

import erp.order.dto.internal.OrderFindRow;
import erp.order.enums.OrderStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record OrderFindAllResponse(
        Long orderId,
        String code,
        LocalDate orderDate,
        String customer,
        Integer itemCount,
        Integer totalAmount,
        OrderStatus status,
        String employeeName,
        LocalDateTime canceledAt,
        String canceledByName
) {
    public static OrderFindAllResponse from(OrderFindRow row) {
        return OrderFindAllResponse.builder()
                .orderId(row.orderId())
                .code(row.code())
                .orderDate(row.orderDate())
                .customer(row.customer())
                .itemCount(row.itemCount())
                .totalAmount(row.totalAmount())
                .status(row.status())
                .employeeName(row.employeeName())
                .canceledAt(row.canceledAt())
                .canceledByName(row.canceledByName())
                .build();
    }
}
