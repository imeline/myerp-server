package erp.outbound.dto.request;

import erp.global.util.time.DatePeriod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import erp.outbound.enums.OutboundStatus;

public record OutboundFindAllRequest(

        @NotNull
        @PositiveOrZero
        Integer page,

        @NotNull
        @Positive
        Integer size,

        // 최근 30일 등 기간 필터 (null 이면 전체)
        DatePeriod period,
        OutboundStatus status,      // null이면 전체(취소 포함)

        // 출고번호(code) 부분 검색
        String code
) {
}
