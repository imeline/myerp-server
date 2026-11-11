package erp.log.dto.response;

import erp.log.dto.internal.LogFindAllRow;
import erp.log.enums.LogType;

public record LogFindAllResponse(
        long logId,
        String createdAt,      // yyyy-MM-dd HH:mm:ss
        String employeeName,   // 없는 계정으로 로그인 시도 등은 null
        LogType type,          // LOGIN | WORK | ERROR
        boolean success,
        String message,
        String ipAddress
) {
    public static LogFindAllResponse from(LogFindAllRow row) {
        return new LogFindAllResponse(
                row.logId(),
                row.createdAt(),
                row.employeeName(),
                LogType.valueOf(row.type()),
                "Y".equals(row.success()),
                row.message(),
                row.ipAddress()
        );
    }
}
