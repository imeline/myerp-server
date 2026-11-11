package erp.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자(ADMIN) 계정 수정 - SYS 전용
 * 회사·로그인 이메일은 바꿀 수 없음 (이메일 = 로그인 아이디). 검증 규칙은 SignupRequest와 같음
 */
public record AdminUpdateRequest(
        @NotBlank
        @Size(min = 1, max = 50, message = "이름은 1~50자입니다.")
        String name,
        @NotBlank
        @Size(max = 32, message = "사번은 최대 32자입니다.")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "사번은 영문/숫자/.-_ 만 허용합니다.")
        String empNo,
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "0\\d{2}-\\d{3,4}-\\d{4}", message = "전화번호는 0XX-XXXX-XXXX 형식이어야 합니다.")
        String phone
) {
}
