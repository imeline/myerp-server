package erp.log.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import erp.auth.security.model.UserPrincipal;
import erp.global.context.RequestContext;
import erp.global.context.TenantContext;
import erp.global.exception.ErrorStatus;
import erp.global.util.SensitiveMasker;
import erp.log.domain.Log;
import erp.global.response.PageResponse;
import erp.global.util.PageParam;
import erp.log.dto.internal.LogSearchFilter;
import erp.log.dto.request.LogFindAllRequest;
import erp.log.dto.request.LogSaveRequest;
import erp.log.dto.response.LogFindAllResponse;
import erp.log.dto.response.LogSummaryFindResponse;
import erp.log.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static erp.global.util.RowCountGuards.requireOneRowAffected;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final int MAX_PAYLOAD_LEN = 32_000;

    private final LogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(LogSaveRequest request) {
        UserPrincipal principal = currentUserOrNull();
        Long tenantId = resolveTenantId(principal);
        String employeeUuid = principal != null ? principal.getUuid() : null;
        String employeeName = principal != null ? principal.getName() : null;
        saveInternal(request, tenantId, employeeUuid, employeeName);
    }

    /**
     * 회사·사용자를 직접 지정해서 저장
     * - 로그인·토큰 재발급처럼 아직 인증 정보(토큰)가 없는 시점에 사용
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAs(LogSaveRequest request, Long tenantId, String employeeUuid, String employeeName) {
        saveInternal(request, tenantId, employeeUuid, employeeName);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LogFindAllResponse> findAllLog(LogFindAllRequest request, long tenantId) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);
        LogSearchFilter filter = LogSearchFilter.from(request);

        List<LogFindAllResponse> responses = logMapper
                .findAllLogFindAllRow(tenantId, filter, pageParam.offset(), pageParam.size())
                .stream()
                .map(LogFindAllResponse::from)
                .toList();
        long total = logMapper.countByFilters(tenantId, filter);

        return PageResponse.of(responses, pageParam.page(), total, pageParam.size());
    }

    @Override
    @Transactional(readOnly = true)
    public LogSummaryFindResponse findSummary(long tenantId) {
        return LogSummaryFindResponse.from(logMapper.findLogSummaryRow(tenantId));
    }

    private void saveInternal(LogSaveRequest request, Long tenantId, String employeeUuid, String employeeName) {
        String ip = RequestContext.ip();
        String requestId = RequestContext.requestId();

        String json = toSafeJson(request.payload());

        long logId = logMapper.nextId();
        Log log = Log.register(
                logId,
                request.type(),
                request.success(),
                employeeUuid,
                employeeName,
                request.message(),
                ip,
                requestId,
                json
        );

        int affected = logMapper.save(tenantId, log);
        requireOneRowAffected(affected, ErrorStatus.LOG_SAVE_FAIL);
    }

    private Long resolveTenantId(UserPrincipal principal) {
        Long t = TenantContext.get(); // "/auth" 등 TenantContext를 설정하지 않는 곳에서는 null
        return (t != null) ? t : (principal != null ? principal.getTenantId() : null);
    }

    private UserPrincipal currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth instanceof AnonymousAuthenticationToken) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof UserPrincipal p) ? p : null;
    }

    /**
     * 민감정보 마스킹 + 직렬화 + 길이 제한
     */
    private String toSafeJson(Object payload) {
        if (payload == null) return null;
        try {
            Object masked = SensitiveMasker.mask(payload);
            String json = objectMapper.writeValueAsString(masked);
            return json.length() > MAX_PAYLOAD_LEN ? json.substring(0, MAX_PAYLOAD_LEN) : json;
        } catch (Exception e) {
            log.warn("payload serialize failed: {}", e.toString());
            return "{\"error\":\"payload-serialize-failed\"}";
        }
    }


}
