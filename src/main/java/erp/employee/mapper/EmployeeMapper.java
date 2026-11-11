package erp.employee.mapper;

import erp.employee.domain.Employee;
import erp.employee.dto.internal.EmployeeFindAllRow;
import erp.employee.dto.internal.EmployeeFindRow;
import erp.employee.dto.internal.EmployeeIdAndNameRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EmployeeMapper {

    long nextId();

    int save(Employee employee);

    List<EmployeeIdAndNameRow> findAllIdAndName(
            @Param("tenantId") long tenantId);

    List<EmployeeFindAllRow> findAllEmployeeFindRow(@Param("tenantId") long tenantId,
                                                    @Param("departmentId") Long departmentId,
                                                    @Param("positionId") Long positionId,
                                                    @Param("name") String name,
                                                    @Param("offset") int offset,
                                                    @Param("size") int size);

    Optional<EmployeeFindRow> findEmployeeFindRowById(@Param("tenantId") long tenantId,
                                                      @Param("employeeId") long employeeId);

    int updateById(@Param("tenantId") long tenantId,
                   @Param("employee") Employee employee);

    int updateStatusToRetired(@Param("tenantId") long tenantId,
                              @Param("employeeId") long employeeId);

    long countByFilters(@Param("tenantId") long tenantId,
                        @Param("departmentId") Long departmentId,
                        @Param("positionId") Long positionId,
                        @Param("name") String name);

    boolean existsByDepartmentId(@Param("tenantId") long tenantId,
                                 @Param("departmentId") long departmentId);

    boolean existsByPositionId(@Param("tenantId") long tenantId,
                               @Param("positionId") long positionId);

    Optional<Long> findActiveEmployeeIdByAccountUuid(@Param("tenantId") long tenantId,
                                                     @Param("accountUuid") String accountUuid);

    boolean existsActiveById(@Param("tenantId") long tenantId,
                             @Param("employeeId") long employeeId);

    boolean existsAnyById(@Param("tenantId") long tenantId,
                          @Param("employeeId") long employeeId);

    boolean existsByEmpNo(@Param("tenantId") long tenantId,
                          @Param("empNo") String empNo,
                          @Param("excludeId") Long excludeId);

    boolean existsByPhone(@Param("tenantId") long tenantId,
                          @Param("phone") String phone,
                          @Param("excludeId") Long excludeId);
}
