// DepartmentServiceImpl.java
package erp.department.service;

import static erp.global.util.RowCountGuards.requireOneRowAffected;

import erp.department.domain.Department;
import erp.department.dto.request.ChildDepartmentSaveRequest;
import erp.department.dto.request.DepartmentUpdateRequest;
import erp.department.dto.request.TopDepartmentSaveRequest;
import erp.department.dto.response.DepartmentInfoResponse;
import erp.department.mapper.DepartmentMapper;
import erp.department.validation.DepartmentValidator;
import erp.employee.validation.EmployeeValidator;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final DepartmentValidator departmentValidator;
    private final EmployeeValidator employeeValidator;

    @Auditable(type = LogType.WORK, messageEl = "'부서 등록(자식): name=' + #args[0].name() + ', parentId=' + #args[0].parentId() + ', tenant=' + #args[1]")
    @Override
    @Transactional
    // 부모가 있는 자식 부서 생성
    public long saveChildDepartment(ChildDepartmentSaveRequest request,
                                    long tenantId) {
        long newDepartmentId = departmentMapper.nextId();
        String name = request.name();
        Long requestedParentId = request.parentId();

        // 중복 이름 검사
        departmentValidator.validNameInParentUnique(tenantId, requestedParentId, name, null);
        // 존재하는 부모 id인지 검사
        departmentValidator.validDepartmentIdIfPresent(requestedParentId, tenantId);

        Department department = Department.register(
                newDepartmentId, name, tenantId, requestedParentId);

        int affectedRowCount = departmentMapper.save(tenantId, department);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_DEPARTMENT_FAIL);
        return newDepartmentId;
    }

    @Auditable(type = LogType.WORK, messageEl = "'부서 등록(루트): name=' + #args[0].name() + ', tenant=' + #args[1]")
    @Override
    @Transactional
    // 부모가 없는 루트 부서 생성
    public long saveTopDepartment(TopDepartmentSaveRequest request,
                                  long tenantId) {
        long newDepartmentId = departmentMapper.nextId();

        String name = request.name();
        Long parentId = null; // 루트 부서는 parent_id 가 NULL

        departmentValidator.validNameInParentUnique(tenantId, parentId, name, null);

        Department department = Department.register(
                newDepartmentId,
                name,
                tenantId,
                null
        );

        int affectedRowCount = departmentMapper.save(tenantId, department);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_DEPARTMENT_FAIL);

        return newDepartmentId;
    }

    // 첫 페이지 로딩: 최상위(parent_id IS NULL)만
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentInfoResponse> findAllTopLevelDepartment(long tenantId) {
        List<DepartmentInfoResponse> list =
                departmentMapper.findAllTopLevelDepartmentRow(tenantId)
                        .stream().map(DepartmentInfoResponse::from).toList();
        return list;
    }

    // 펼침 클릭: 특정 parent의 직계 자식만
    public List<DepartmentInfoResponse> findAllByParentId(long parentId,
                                                          long tenantId) {
        List<DepartmentInfoResponse> list =
                departmentMapper.findAllDepartmentRowByParentId(tenantId, parentId)
                        .stream().map(DepartmentInfoResponse::from).toList();
        return list;
    }

    @Auditable(type = LogType.WORK, messageEl = "'부서명 변경: id=' + #args[0] + ' → ' + #args[1].name() + ', tenant=' + #args[2]")
    @Override
    @Transactional
    public void updateDepartment(Long departmentId,
                                 DepartmentUpdateRequest request,
                                 long tenantId) {

        Department curDepartment = departmentMapper.findById(tenantId, departmentId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_DEPARTMENT));

        Long parentId = curDepartment.getParentId();
        String newName = request.name();
        departmentValidator.validNameInParentUnique(tenantId, parentId, newName, departmentId);

        Department department = curDepartment.changeName(newName);
        int affectedRowCount = departmentMapper.updateById(tenantId, department);
        requireOneRowAffected(affectedRowCount, ErrorStatus.UPDATE_DEPARTMENT_FAIL);
    }

    @Auditable(type = LogType.WORK, messageEl = "'부서 삭제: id=' + #args[0] + ', tenant=' + #args[1]")
    @Override
    @Transactional
    public void deleteDepartment(Long departmentId, long tenantId) {
        departmentValidator.validNoChildDepartments(departmentId, tenantId);
        employeeValidator.validNoEmployeesInDepartment(departmentId, tenantId);

        int affectedRowCount = departmentMapper.deleteById(tenantId, departmentId);
        requireOneRowAffected(affectedRowCount, ErrorStatus.DELETE_DEPARTMENT_FAIL);
    }
}
