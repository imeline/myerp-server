package erp.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 계정 등록 요청 (회사 정보 없음)
 * - ADMIN이 자기 회사 사원(USER)을 등록할 때 사용. companyId는 토큰(TenantContext)에서 꺼낸다.
 */
public record SignupRequest(
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
                // KR 로컬(하이픈/공백 선택) 또는 E.164(+82) 허용
                regexp = "0\\d{2}-\\d{3,4}-\\d{4}",
                message = "전화번호는 0XX-XXXX-XXXX 형식이어야 합니다."
        )
        String phone,
        @NotNull
        Long departmentId,
        @NotNull
        Long positionId
) {

}


