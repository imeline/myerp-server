package erp.log.dto.internal;

import erp.log.dto.request.LogFindAllRequest;
import erp.log.enums.LogType;

import java.time.LocalDate;

import static erp.global.util.Strings.normalizeOrNull;

/**
 * 로그 검색 조건 (SQL에 바로 넘길 수 있게 정리한 값)
 */
public record LogSearchFilter(
        String type,              // 'LOGIN' | 'WORK' | 'ERROR' | null
        String successYn,         // 'Y' | 'N' | null
        String employeeName,
        String keyword,
        LocalDate startDate,      // 이상
        LocalDate endDateExclusive // 미만 (종료일 + 1일)
) {
    public static LogSearchFilter from(LogFindAllRequest request) {
        LogType type = request.type();
        Boolean success = request.success();
        return new LogSearchFilter(
                type == null ? null : type.name(),
                success == null ? null : (success ? "Y" : "N"),
                normalizeOrNull(request.employeeName()),
                normalizeOrNull(request.keyword()),
                request.startDate(),
                request.endDate() == null ? null : request.endDate().plusDays(1)
        );
    }
}
