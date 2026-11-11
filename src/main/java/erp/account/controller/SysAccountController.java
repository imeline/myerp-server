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
import erp.account.dto.request.AdminFindAllRequest;
import erp.account.dto.response.AdminFindAllResponse;
import erp.account.service.AdminAccountService;
import erp.global.response.PageResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import erp.account.dto.request.AdminUpdateRequest;
import erp.account.dto.response.AdminFindResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

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
    private final AdminAccountService adminAccountService;

    /** 관리자(ADMIN) 계정 목록 - 회사명·이름 부분 검색, 페이지 */
    @GetMapping("/admin")
    public ApiResponse<PageResponse<AdminFindAllResponse>> findAllAdmin(
            @Valid @ParameterObject @ModelAttribute AdminFindAllRequest request) {
        return ApiResponse.onSuccess(adminAccountService.findAllAdmin(request));
    }

    /** 관리자(ADMIN) 계정 단건 (수정 모달용) */
    @GetMapping("/admin/{erpAccountId}")
    public ApiResponse<AdminFindResponse> findAdmin(@PathVariable long erpAccountId) {
        return ApiResponse.onSuccess(adminAccountService.findAdmin(erpAccountId));
    }

    /** 관리자(ADMIN) 계정 수정 - 이름·사번·연락처 (회사·이메일은 변경 불가) */
    @PutMapping("/admin/{erpAccountId}")
    public ApiResponse<Void> updateAdmin(@PathVariable long erpAccountId,
                                         @Valid @RequestBody AdminUpdateRequest request) {
        adminAccountService.updateAdmin(erpAccountId, request);
        return ApiResponse.onSuccess(null);
    }

    /** 관리자(ADMIN) 계정 삭제 - 사원 퇴사 처리 + 계정 소프트 삭제 */
    @DeleteMapping("/admin/{erpAccountId}")
    public ApiResponse<Void> deleteAdmin(@PathVariable long erpAccountId) {
        adminAccountService.deleteAdmin(erpAccountId);
        return ApiResponse.onSuccess(null);
    }

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
