package erp.log.dto.response;

import erp.log.dto.internal.LogSummaryRow;

/**
 * 시스템 로그 상단 카드 (자기 회사, 접속(ACCESS) 로그 제외)
 */
public record LogSummaryFindResponse(
        long todaySuccessCount,    // 오늘 성공 로그 수
        long todayFailCount,       // 오늘 실패·오류 로그 수
        long todayLoginUserCount,  // 오늘 로그인한 사용자 수 (중복 제외)
        long recentWorkCount       // 최근 1시간 작업 로그 수
) {
    public static LogSummaryFindResponse from(LogSummaryRow row) {
        return new LogSummaryFindResponse(
                nz(row.todaySuccessCount()),
                nz(row.todayFailCount()),
                nz(row.todayLoginUserCount()),
                nz(row.recentWorkCount())
        );
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
