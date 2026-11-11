package erp.purchase.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record PurchaseSaveRequest(
        @NotNull
        LocalDate purchaseDate,
        @NotBlank
        String supplier,
        @NotEmpty
        @Valid
        List<PurchaseItemSaveRequest> items
) {
}
