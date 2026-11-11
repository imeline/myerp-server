package erp.auth.service;

import erp.account.enums.ErpAccountRole;
import erp.auth.dto.request.LoginRequest;
import erp.auth.dto.request.RefreshTokenRequest;
import erp.auth.dto.request.SignupRequest;
import erp.auth.dto.response.LoginResponse;

public interface AuthService {
    /**
     * 계정(직원 + ERP 계정) 등록
     * @param companyId 등록할 회사. 호출하는 쪽(컨트롤러)이 권한에 맞게 정해서 넘긴다.
     */
    void signup(SignupRequest dto, long companyId, ErpAccountRole role);

    LoginResponse login(LoginRequest dto);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(String uuid,
                RefreshTokenRequest request);
}
