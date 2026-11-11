package erp.account.controller;

import erp.account.enums.ErpAccountRole;
import erp.auth.dto.request.SignupWithCompanyRequest;
import erp.auth.service.AuthService;
import erp.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SYS 전용 계정 등록 (/api/v1/sys/** 는 SecurityConfig에서 ROLE_SYS만 허용)
 * <p>
 * 역할(role)은 요청 body가 아니라 컨트롤러에서 정한다.
 * - 서비스는 "어떤 역할인지"와 무관하게 가입 로직만 수행 (관심사 분리)
 * - 요청 body로 받으면 사용자가 조작할 수 있어 보안상 위험
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sys")
public class SysAccountController {

    private final AuthService authService;

    /** 관리자(ADMIN) 계정 등록 - SYS가 대상 회사를 선택 */
    @PostMapping("/admin")
    public ApiResponse<Void> saveAdminAccount(@Valid @RequestBody SignupWithCompanyRequest request) {
        authService.signup(request.toSignupRequest(), request.companyId(), ErpAccountRole.ADMIN);
        return ApiResponse.onSuccess(null);
    }

    /** 시스템 관리자(SYS) 계정 등록 */
    @PostMapping("/sys-account")
    public ApiResponse<Void> saveSysAccount(@Valid @RequestBody SignupWithCompanyRequest request) {
        authService.signup(request.toSignupRequest(), request.companyId(), ErpAccountRole.SYS);
        return ApiResponse.onSuccess(null);
    }
}
