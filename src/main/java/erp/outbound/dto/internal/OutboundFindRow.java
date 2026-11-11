package erp.outbound.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalDateTime;
import erp.outbound.enums.OutboundStatus;

public record OutboundFindRow(

        @NotNull
        Long outboundId,

        @NotBlank
        String code,

        @NotBlank
        String customer,

        @NotNull
        @Positive
        Integer itemCount,

        @NotNull
        @PositiveOrZero
        Integer totalAmount,

        @NotNull
        LocalDate outboundDate,

        @NotNull
        Long orderId,

        @NotBlank
        String orderCode,

        @NotBlank
        String employeeName,

        @NotNull
        OutboundStatus status,

        LocalDateTime canceledAt,   // 취소 전엔 null
        String canceledByName       // 취소 전엔 null
) {
}
