package erp.account.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자(ADMIN) 계정 목록 한 줄
 * 주의: MyBatis는 record를 SELECT 컬럼 "순서"로 매핑 → ErpAccountMapper.xml findAllAdminFindAllRow의 SELECT 순서와 맞출 것
 */
public record AdminFindAllRow(
        @NotNull
        Long erpAccountId,
        @NotNull
        Long employeeId,
        @NotBlank
        String name,
        @NotBlank
        String loginEmail,
        @NotNull
        Long companyId,
        @NotBlank
        String companyName,
        String phone,
        @NotBlank
        String createdAt
) {
}
