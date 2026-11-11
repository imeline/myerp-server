package erp.employee.service;

import erp.employee.dto.request.EmployeeFindAllRequest;
import erp.employee.dto.request.EmployeeSaveRequest;
import erp.employee.dto.request.EmployeeUpdateRequest;
import erp.employee.dto.response.EmployeeFindAllResponse;
import erp.employee.dto.response.EmployeeFindResponse;
import erp.employee.dto.response.EmployeeIdAndNameResponse;
import erp.global.response.PageResponse;
import java.util.List;

public interface EmployeeService {

    Long saveEmployee(EmployeeSaveRequest request, long companyId);

    List<EmployeeIdAndNameResponse> findAllIdAndName(long tenantId);

    PageResponse<EmployeeFindAllResponse> findAllEmployees(
        EmployeeFindAllRequest request, long tenantId);

    EmployeeFindResponse findEmployee(long employeeId, long tenantId);

    void updateEmployee(long employeeId, EmployeeUpdateRequest request, long tenantId);

    void retireEmployee(long employeeId, long tenantId);

    /** 이름·사번·연락처만 수정 (부서·직급은 그대로). 사번·연락처는 회사 안에서 중복 불가 */
    void updateEmployeeBasicInfo(long employeeId, String name, String empNo, String phone, long tenantId);

    /** 로그인 계정(uuid)의 재직 중인 사원 ID. 없으면(삭제·퇴사) NOT_FOUND_EMPLOYEE */
    long findActiveEmployeeIdByAccountUuid(String accountUuid, long tenantId);
}
