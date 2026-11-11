package erp.inbound.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InboundSaveRequest(
        @NotNull
        Long purchaseId,
        @NotNull
        LocalDate inboundDate
) {
}
