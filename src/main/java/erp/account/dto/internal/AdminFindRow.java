package erp.account.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자(ADMIN) 계정 단건 (수정 모달·수정·삭제 대상 확인용)
 * 주의: MyBatis는 record를 SELECT 컬럼 "순서"로 매핑 → ErpAccountMapper.xml findAdminFindRowById의 SELECT 순서와 맞출 것
 */
public record AdminFindRow(
        @NotNull
        Long erpAccountId,
        @NotNull
        Long employeeId,
        @NotBlank
        String name,
        @NotBlank
        String empNo,
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
