package erp.log.dto.request;

import erp.log.enums.LogType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 시스템 로그 목록 조회 조건 (ADMIN, 자기 회사만)
 * - 접속(ACCESS) 로그는 양이 많아 화면에서 제외 → 조회 불가
 */
public record LogFindAllRequest(
        LogType type,              // LOGIN | WORK | ERROR (null이면 셋 다)
        Boolean success,           // true=성공, false=실패 (null이면 전체)
        String employeeName,       // 사용자 이름 부분 검색
        String keyword,            // 로그 내용 부분 검색
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,       // 시작일(포함), yyyy-MM-dd
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,         // 종료일(포함), yyyy-MM-dd
        @PositiveOrZero
        Integer page,              // 0-base, null이면 0
        @Positive
        Integer size               // null이면 20
) {
    @AssertTrue(message = "접속(ACCESS) 로그는 조회할 수 없습니다.")
    public boolean isNotAccessType() {
        return type != LogType.ACCESS;
    }

    @AssertTrue(message = "시작일은 종료일보다 늦을 수 없습니다.")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) return true;
        return !startDate.isAfter(endDate);
    }
}
