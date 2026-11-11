package erp.purchase.dto.internal;

import erp.purchase.enums.PurchaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseFindRow(
        @NotNull
        Long purchaseId,
        @NotNull
        String code,
        @NotNull
        String supplier,
        @NotNull
        @Positive
        Integer itemCount,
        @NotNull
        @Positive
        Integer totalAmount,
        @NotNull
        LocalDate purchaseDate,
        @NotNull
        PurchaseStatus status,
        @NotBlank
        String employeeName,
        LocalDateTime canceledAt,   // 취소 전엔 null
        String canceledByName       // 취소 전엔 null
) {

}
