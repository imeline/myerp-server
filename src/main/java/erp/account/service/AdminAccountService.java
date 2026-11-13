package erp.account.service;

import erp.account.dto.request.AdminFindAllRequest;
import erp.account.dto.request.AdminUpdateRequest;
import erp.account.dto.response.AdminFindAllResponse;
import erp.account.dto.response.AdminFindResponse;
import erp.global.response.PageResponse;
import erp.auth.dto.response.PasswordResetResponse;

/**
 * 관리자(ADMIN) 계정 관리 - SYS 전용 (회사 구분 없이 전체)
 * ErpAccountService와 분리한 이유: EmployeeService가 ErpAccountService를 쓰므로,
 * 여기서 EmployeeService를 쓰면 서로 참조하는 순환이 생김
 */
public interface AdminAccountService {

    /** 목록 - 회사명·이름 부분 검색 (삭제된 계정·삭제된 회사 제외) */
    PageResponse<AdminFindAllResponse> findAllAdmin(AdminFindAllRequest request);

    /** 단건 (수정 모달용) */
    AdminFindResponse findAdmin(long erpAccountId);

    /** 이름·사번·연락처 수정 */
    void updateAdmin(long erpAccountId, AdminUpdateRequest request);

    /** 비밀번호를 임시 비밀번호로 초기화 (임시 비밀번호는 응답으로 한 번만) */
    PasswordResetResponse resetPassword(long erpAccountId);

    /** 삭제 - 사원 퇴사 처리 + 계정 소프트 삭제 (기록은 남음) */
    void deleteAdmin(long erpAccountId);
}
