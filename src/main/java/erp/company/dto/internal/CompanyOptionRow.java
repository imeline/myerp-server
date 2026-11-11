package erp.company.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 회사 선택 옵션 (관리자 계정 등록 등) */
public record CompanyOptionRow(
        @NotNull
        Long companyId,
        @NotBlank
        String name
) {
}
