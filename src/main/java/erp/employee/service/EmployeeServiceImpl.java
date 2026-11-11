// EmployeeServiceImpl.java
package erp.employee.service;

import erp.account.service.ErpAccountService;
import erp.department.validation.DepartmentValidator;
import erp.employee.domain.Employee;
import erp.employee.dto.internal.EmployeeFindAllRow;
import erp.employee.dto.internal.EmployeeIdAndNameRow;
import erp.employee.dto.request.EmployeeFindAllRequest;
import erp.employee.dto.request.EmployeeSaveRequest;
import erp.employee.dto.request.EmployeeUpdateRequest;
import erp.employee.dto.response.EmployeeFindAllResponse;
import erp.employee.dto.response.EmployeeFindResponse;
import erp.employee.dto.response.EmployeeIdAndNameResponse;
import erp.employee.enums.EmployeeStatus;
import erp.employee.mapper.EmployeeMapper;
import erp.employee.validation.EmployeeValidator;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.PageParam;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import erp.position.validation.PositionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static erp.global.util.RowCountGuards.requireOneRowAffected;
import static erp.global.util.Strings.normalizeOrNull;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final ErpAccountService erpAccountService;
    private final DepartmentValidator departmentValidator;
    private final PositionValidator positionValidator;
    private final EmployeeValidator employeeValidator;
    
    @Override
    @Transactional
    public Long saveEmployee(EmployeeSaveRequest request, long companyId) {
        employeeValidator.validEmpNoUnique(request.empNo(), companyId, null);
        employeeValidator.validPhoneUnique(request.phone(), companyId, null);
        departmentValidator.validDepartmentIdIfPresent(request.departmentId(), companyId);
        positionValidator.validPositionIdIfPresent(request.positionId(), companyId);

        long employeeId = employeeMapper.nextId();

        Employee employee = Employee.register(
                employeeId,
                request.empNo(),
                request.name(),
                request.phone(),
                EmployeeStatus.ACTIVE,
                request.departmentId(),
                request.positionId(),
                companyId
        );

        int affectedRowCount = employeeMapper.save(employee);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_EMPLOYEE_FAIL);

        return employeeId;
    }


    @Override
    @Transactional(readOnly = true)
    public List<EmployeeIdAndNameResponse> findAllIdAndName(long tenantId) {
        List<EmployeeIdAndNameRow> employeeIdAndNameRows =
                employeeMapper.findAllIdAndName(tenantId);

        return employeeIdAndNameRows.stream()
                .map(EmployeeIdAndNameResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeFindAllResponse> findAllEmployees(EmployeeFindAllRequest request,
                                                                  long tenantId) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);

        Long departmentId = request.departmentId();
        Long positionId = request.positionId();
        String name = normalizeOrNull(request.name());

        List<EmployeeFindAllRow> rows = employeeMapper.findAllEmployeeFindRow(
                tenantId, departmentId, positionId, name, pageParam.offset(), pageParam.size());

        List<EmployeeFindAllResponse> responses = rows.stream()
                .map(EmployeeFindAllResponse::from)
                .toList();

        long total = employeeMapper.countByFilters(tenantId, departmentId, positionId, name);

        return PageResponse.of(responses, pageParam.page(), total, pageParam.size());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeFindResponse findEmployee(long employeeId, long tenantId) {
        return employeeMapper.findEmployeeFindRowById(tenantId, employeeId)
                .map(EmployeeFindResponse::from)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_EMPLOYEE));
    }

    @Auditable(type = LogType.WORK, messageEl = "'직원 수정: id=' + #args[0] + ', empNo=' + #args[1].empNo() + ', name=' + #args[1].name() + ', tenant=' + #args[2]")
    @Override
    @Transactional
    public void updateEmployee(long employeeId, EmployeeUpdateRequest request,
                               long tenantId) {
        String name = normalizeOrNull(request.name());
        String empNo = normalizeOrNull(request.empNo());
        String phone = normalizeOrNull(request.phone());
        Long departmentId = request.departmentId();
        Long positionId = request.positionId();

        departmentValidator.validDepartmentIdIfPresent(departmentId, tenantId);
        positionValidator.validPositionIdIfPresent(positionId, tenantId);
        employeeValidator.validEmpNoUnique(empNo, tenantId, employeeId);
        employeeValidator.validPhoneUnique(phone, tenantId, employeeId);

        Employee employee = Employee.update(
                employeeId,
                empNo,
                name,
                phone,
                request.departmentId(),
                request.positionId(),
                tenantId
        );

        int affectedRowCount = employeeMapper.updateById(tenantId, employee);
        requireOneRowAffected(affectedRowCount, ErrorStatus.UPDATE_EMPLOYEE_FAIL);
    }

    @Auditable(type = LogType.WORK, messageEl = "'직원 퇴사 처리: id=' + #args[0] + ', tenant=' + #args[1]")
    @Override
    @Transactional
    public void retireEmployee(long employeeId, long tenantId) {
        int affected = employeeMapper.updateStatusToRetired(tenantId, employeeId);
        if (affected == 1) {
            erpAccountService.softDeleteById(employeeId, tenantId);
            return;
        }
        if (employeeMapper.existsAnyById(tenantId, employeeId)) {
            throw new GlobalException(ErrorStatus.ALREADY_RETIRED_EMPLOYEE);
        }
        throw new GlobalException(ErrorStatus.NOT_FOUND_EMPLOYEE);
    }


    @Override
    @Transactional(readOnly = true)
    public long findActiveEmployeeIdByAccountUuid(String accountUuid, long tenantId) {
        return employeeMapper.findActiveEmployeeIdByAccountUuid(tenantId, accountUuid)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_EMPLOYEE));
    }
}
