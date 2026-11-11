package erp.outbound.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OutboundSaveRequest(

        @NotNull
        LocalDate outboundDate,

        @NotNull
        Long orderId
) {
}