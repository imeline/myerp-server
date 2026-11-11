package erp.log.dto.internal;

/**
 * 로그 요약(상단 카드) 집계 행
 * 주의: SELECT 컬럼 순서를 이 record 필드 순서와 맞출 것 (LogMapper.xml findLogSummaryRow)
 */
public record LogSummaryRow(
        Long todaySuccessCount,
        Long todayFailCount,
        Long todayLoginUserCount,
        Long recentWorkCount
) {
}
