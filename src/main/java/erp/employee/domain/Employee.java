package erp.employee.domain;

import erp.employee.enums.EmployeeStatus;
import erp.global.base.TimeStamped;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Employee extends TimeStamped {
    private Long employeeId;
    private String empNo;
    private String name;
    private String phone;
    private EmployeeStatus status;
    private Long departmentId;
    private Long positionId;
    private long companyId;

    public static Employee register(Long employeeId,
                                    String empNo, String name, String phone,
                                    EmployeeStatus status, Long departmentId,
                                    Long positionId, long companyId // 부서·직급은 null 허용 (SYS의 관리자 등록)
    ) {
        return Employee.builder()
                .employeeId(employeeId)
                .empNo(empNo)
                .name(name)
                .phone(phone)
                .status(status)
                .departmentId(departmentId)
                .positionId(positionId)
                .companyId(companyId)
                .build();
    }

    public static Employee update(Long employeeId,
                                  String empNo,
                                  String name,
                                  String phone,
                                  Long departmentId,
                                  Long positionId,
                                  long companyId) {
        return Employee.builder()
                .employeeId(employeeId)
                .name(name)
                .empNo(empNo)
                .departmentId(departmentId)
                .positionId(positionId)
                .phone(phone)
                .companyId(companyId)
                .build();
    }
}
