package erp.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회사를 지정하는 계정 등록 요청
 * - SYS가 ADMIN·SYS 계정을 등록할 때 사용. SYS는 여러 회사를 관리하므로 대상 회사를 body로 받는다.
 * - 부서·직급은 선택 입력 (SYS는 회사의 부서·직급을 조회할 권한이 없음). 보내면 그 회사에 있는지 검증한다.
 */
public record SignupWithCompanyRequest(
        @NotBlank
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 254)
        String loginEmail,
        @NotBlank
        @Size(min = 6, max = 72, message = "비밀번호는 최소 6자 이상(최대 72자)이어야 합니다.")
        String password,
        @NotBlank
        @Size(max = 32, message = "사번은 최대 32자입니다.")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "사번은 영문/숫자/.-_ 만 허용합니다.")
        String empNo,
        @NotBlank
        @Size(min = 1, max = 50, message = "이름은 1~50자입니다.")
        String name,
        @NotBlank
        @Size(max = 20)
        @Pattern(
                regexp = "0\\d{2}-\\d{3,4}-\\d{4}",
                message = "전화번호는 0XX-XXXX-XXXX 형식이어야 합니다."
        )
        String phone,
        Long departmentId,
        Long positionId,
        @NotNull
        Long companyId
) {

    public SignupRequest toSignupRequest() {
        return new SignupRequest(loginEmail, password, empNo, name, phone, departmentId, positionId);
    }
}
