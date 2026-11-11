package erp.log.dto.internal;

/**
 * 로그 목록 조회 행
 * 주의: MyBatis는 record를 SELECT 컬럼 "순서"로 매핑 → LogMapper.xml findAllLogFindAllRow의 SELECT 순서와 맞출 것
 */
public record LogFindAllRow(
        Long logId,
        String createdAt,     // yyyy-MM-dd HH:mm:ss
        String employeeName,
        String type,
        String success,       // 'Y' | 'N'
        String message,
        String ipAddress
) {
}
