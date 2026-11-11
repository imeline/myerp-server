package erp.log.service;

import erp.global.response.PageResponse;
import erp.log.dto.request.LogFindAllRequest;
import erp.log.dto.request.LogSaveRequest;
import erp.log.dto.response.LogFindAllResponse;
import erp.log.dto.response.LogSummaryFindResponse;

public interface LogService {
    void save(LogSaveRequest request);

    /** 회사·사용자를 직접 지정해서 저장 (로그인·토큰 재발급처럼 토큰이 없는 시점용) */
    void saveAs(LogSaveRequest request, Long tenantId, String employeeUuid, String employeeName);

    /** 시스템 로그 목록 (자기 회사, 접속 로그 제외) */
    PageResponse<LogFindAllResponse> findAllLog(LogFindAllRequest request, long tenantId);

    /** 시스템 로그 상단 카드 (자기 회사, 접속 로그 제외) */
    LogSummaryFindResponse findSummary(long tenantId);
}
