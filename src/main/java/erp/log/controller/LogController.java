package erp.log.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import erp.log.dto.request.LogFindAllRequest;
import erp.log.dto.response.LogFindAllResponse;
import erp.log.dto.response.LogSummaryFindResponse;
import erp.log.service.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시스템 로그 조회 - ADMIN 전용 (/api/v1/admin/** 는 SecurityConfig에서 ROLE_ADMIN만 허용)
 * 회사는 토큰 기준(TenantContext)으로 고정 → 다른 회사 로그는 볼 수 없음
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/log")
public class LogController {

    private final LogService logService;

    /**
     * 시스템 로그 목록 조회
     */
    @GetMapping
    public ApiResponse<PageResponse<LogFindAllResponse>> findAllLogs(
            @Valid @ParameterObject @ModelAttribute LogFindAllRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(logService.findAllLog(request, tenantId));
    }

    /**
     * 시스템 로그 현황 조회 (상단 카드)
     */
    @GetMapping("/summary")
    public ApiResponse<LogSummaryFindResponse> findSummary() {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(logService.findSummary(tenantId));
    }
}
