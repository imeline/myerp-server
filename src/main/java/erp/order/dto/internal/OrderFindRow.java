package erp.order.dto.internal;

import erp.order.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderFindRow(
        @NotNull
        Long orderId,
        @NotBlank
        String code,
        @NotNull
        LocalDate orderDate,
        @NotBlank
        String customer,
        @NotNull
        Integer itemCount,
        @NotNull
        Integer totalAmount,
        @NotNull
        OrderStatus status,
        @NotBlank
        String employeeName,
        LocalDateTime canceledAt,   // 취소 전엔 null
        String canceledByName       // 취소 전엔 null
) {
}
