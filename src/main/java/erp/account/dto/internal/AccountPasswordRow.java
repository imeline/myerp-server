package erp.account.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 비밀번호 변경용 계정 정보 (삭제되지 않은 계정)
 * 주의: MyBatis는 record를 SELECT 컬럼 "순서"로 매핑 → ErpAccountMapper.xml findAccountPasswordRowByUuid의 SELECT 순서와 맞출 것
 */
public record AccountPasswordRow(
        @NotNull
        Long erpAccountId,
        @NotBlank
        String passwordHash
) {
}
