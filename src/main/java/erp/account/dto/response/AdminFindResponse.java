package erp.account.dto.response;

import erp.account.dto.internal.AdminFindRow;
import lombok.Builder;

@Builder
public record AdminFindResponse(
        long erpAccountId,
        long employeeId,
        String name,
        String empNo,
        String loginEmail,
        long companyId,
        String companyName,
        String phone,
        String createdAt
) {

    public static AdminFindResponse from(AdminFindRow row) {
        return AdminFindResponse.builder()
                .erpAccountId(row.erpAccountId())
                .employeeId(row.employeeId())
                .name(row.name())
                .empNo(row.empNo())
                .loginEmail(row.loginEmail())
                .companyId(row.companyId())
                .companyName(row.companyName())
                .phone(row.phone())
                .createdAt(row.createdAt())
                .build();
    }
}
