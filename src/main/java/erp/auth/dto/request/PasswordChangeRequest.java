package erp.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 내 비밀번호 변경 (로그인한 모든 사용자) */
public record PasswordChangeRequest(
        @NotBlank
        String currentPassword,
        @NotBlank
        @Size(min = 6, max = 72, message = "비밀번호는 최소 6자 이상(최대 72자)이어야 합니다.")
        String newPassword
) {
}
