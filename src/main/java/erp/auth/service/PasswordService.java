package erp.auth.service;

import erp.auth.dto.request.PasswordChangeRequest;
import erp.auth.dto.response.PasswordResetResponse;

/**
 * 비밀번호 변경·초기화
 * - 변경·초기화하면 그 계정의 리프레시 토큰을 지워 다른 곳의 로그인 세션을 끊음
 */
public interface PasswordService {

    /** 내 비밀번호 변경 (현재 비밀번호 확인) */
    void changeMyPassword(String uuid, PasswordChangeRequest request);

    /** 계정 비밀번호를 임시 비밀번호로 초기화 (권한 확인은 호출하는 쪽에서) */
    PasswordResetResponse resetPassword(long erpAccountId);

    /** ADMIN이 자기 회사 사원의 비밀번호 초기화 (SYS 계정·다른 회사 사원은 불가) */
    PasswordResetResponse resetEmployeePassword(long employeeId, long tenantId);
}
