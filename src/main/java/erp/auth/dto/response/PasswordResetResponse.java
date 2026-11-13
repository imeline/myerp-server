package erp.auth.dto.response;

/** 비밀번호 초기화 결과 - 임시 비밀번호는 이 응답에서 한 번만 보여줌 (저장은 암호화된 값만) */
public record PasswordResetResponse(
        String temporaryPassword
) {
}
