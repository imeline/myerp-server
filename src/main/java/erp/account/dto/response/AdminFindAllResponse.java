package erp.account.dto.response;

import erp.account.dto.internal.AdminFindAllRow;
import lombok.Builder;

@Builder
public record AdminFindAllResponse(
        long erpAccountId,
        long employeeId,
        String name,
        String loginEmail,
        long companyId,
        String companyName,
        String phone,
        String createdAt
) {

    public static AdminFindAllResponse from(AdminFindAllRow row) {
        return AdminFindAllResponse.builder()
                .erpAccountId(row.erpAccountId())
                .employeeId(row.employeeId())
                .name(row.name())
                .loginEmail(row.loginEmail())
                .companyId(row.companyId())
                .companyName(row.companyName())
                .phone(row.phone())
                .createdAt(row.createdAt())
                .build();
    }
}
