package erp.company.dto.response;

import erp.company.dto.internal.CompanyOptionRow;
import lombok.Builder;

@Builder
public record CompanyOptionResponse(
        long companyId,
        String name
) {

    public static CompanyOptionResponse from(CompanyOptionRow row) {
        return CompanyOptionResponse.builder()
                .companyId(row.companyId())
                .name(row.name())
                .build();
    }
}
