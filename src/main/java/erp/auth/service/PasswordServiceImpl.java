package erp.auth.service;

import erp.account.dto.internal.AccountPasswordRow;
import erp.account.mapper.ErpAccountMapper;
import erp.auth.dto.request.PasswordChangeRequest;
import erp.auth.dto.response.PasswordResetResponse;
import erp.auth.mapper.RefreshTokenMapper;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

import static erp.global.util.RowCountGuards.requireOneRowAffected;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    // 헷갈리는 글자(0/O, 1/l/I) 제외
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ErpAccountMapper erpAccountMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;

    @Auditable(type = LogType.WORK, messageEl = "'비밀번호 변경'")
    @Override
    @Transactional
    public void changeMyPassword(String uuid, PasswordChangeRequest request) {
        AccountPasswordRow account = erpAccountMapper.findAccountPasswordRowByUuid(uuid)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ERP_ACCOUNT));

        if (!passwordEncoder.matches(request.currentPassword(), account.passwordHash())) {
            throw new GlobalException(ErrorStatus.INVALID_CURRENT_PASSWORD);
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new GlobalException(ErrorStatus.SAME_AS_CURRENT_PASSWORD);
        }
        updatePassword(account.erpAccountId(), request.newPassword());
    }

    @Auditable(type = LogType.WORK, messageEl = "'비밀번호 초기화: erpAccountId=' + #args[0]")
    @Override
    @Transactional
    public PasswordResetResponse resetPassword(long erpAccountId) {
        String temporary = generateTemporaryPassword();
        updatePassword(erpAccountId, temporary);
        return new PasswordResetResponse(temporary);
    }

    @Override
    @Transactional
    public PasswordResetResponse resetEmployeePassword(long employeeId, long tenantId) {
        long erpAccountId = erpAccountMapper.findResettableErpAccountIdByEmployeeId(tenantId, employeeId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ERP_ACCOUNT));
        return resetPassword(erpAccountId);
    }

    /** 비밀번호 저장(암호화) + 그 계정의 로그인 세션(리프레시 토큰) 끊기 */
    private void updatePassword(long erpAccountId, String rawPassword) {
        int affected = erpAccountMapper.updatePasswordById(erpAccountId, passwordEncoder.encode(rawPassword));
        requireOneRowAffected(affected, ErrorStatus.UPDATE_PASSWORD_FAIL);
        refreshTokenMapper.deleteByErpAccountId(erpAccountId);
    }

    /** 임시 비밀번호: 영문 8 + 숫자 2, 섞어서 10자리 */
    private static String generateTemporaryPassword() {
        char[] chars = new char[10];
        for (int i = 0; i < 8; i++) chars[i] = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        for (int i = 8; i < 10; i++) chars[i] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = chars[i];
            chars[i] = chars[j];
            chars[j] = t;
        }
        return new String(chars);
    }
}
