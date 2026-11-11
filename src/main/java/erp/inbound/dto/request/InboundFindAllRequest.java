package erp.inbound.dto.request;

import erp.global.util.time.DatePeriod;
import jakarta.validation.constraints.NotNull;
import erp.inbound.enums.InboundStatus;

public record InboundFindAllRequest(
        DatePeriod period,
        InboundStatus status,      // null이면 전체(취소 포함)
        String code,
        @NotNull
        Integer page,
        @NotNull
        Integer size
) {
}
