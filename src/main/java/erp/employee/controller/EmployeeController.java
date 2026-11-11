package erp.employee.controller;

import erp.employee.dto.request.EmployeeFindAllRequest;
import erp.employee.dto.request.EmployeeUpdateRequest;
import erp.employee.dto.response.EmployeeFindAllResponse;
import erp.employee.dto.response.EmployeeFindResponse;
import erp.employee.dto.response.EmployeeIdAndNameResponse;
import erp.employee.service.EmployeeService;
import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/id-name")
    public ApiResponse<List<EmployeeIdAndNameResponse>> findAllIdAndName() {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(employeeService.findAllIdAndName(tenantId));
    }

    @GetMapping
    public ApiResponse<PageResponse<EmployeeFindAllResponse>> findAllEmployees(
            @Valid @ParameterObject @ModelAttribute EmployeeFindAllRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                employeeService.findAllEmployees(request, tenantId));
    }

    @GetMapping("/{employeeId}")
    public ApiResponse<EmployeeFindResponse> findEmployee(@PathVariable Long employeeId) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(employeeService.findEmployee(employeeId, tenantId));
    }

    @PutMapping("/{employeeId}")
    public ApiResponse<Void> updateEmployee(@PathVariable Long employeeId,
                                            @Valid @RequestBody EmployeeUpdateRequest request) {
        long tenantId = TenantContext.get();
        employeeService.updateEmployee(employeeId, request, tenantId);
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/{employeeId}/retire")
    public ApiResponse<Void> retireEmployee(@PathVariable Long employeeId) {
        long tenantId = TenantContext.get();
        employeeService.retireEmployee(employeeId, tenantId);
        return ApiResponse.onSuccess(null);
    }
}
