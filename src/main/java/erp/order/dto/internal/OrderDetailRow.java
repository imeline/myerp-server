package erp.order.dto.internal;

import erp.order.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderDetailRow(
        @NotBlank
        String code,

        @NotBlank
        String customer,

        @NotNull
        LocalDate orderDate,

        @NotNull
        OrderStatus status,

        @NotBlank
        String employeeName,

        @NotNull
        @Positive
        Integer totalItemCount,

        @NotNull
        @Positive
        Integer totalQuantity,

        @NotNull
        @Positive
        Integer totalAmount,
        LocalDateTime canceledAt,   // 취소 전엔 null
        String canceledByName       // 취소 전엔 null
) {
}
