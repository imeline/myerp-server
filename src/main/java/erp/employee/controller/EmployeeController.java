package erp.employee.controller;

import erp.account.enums.ErpAccountRole;
import erp.auth.dto.request.SignupRequest;
import erp.auth.service.AuthService;
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
    private final AuthService authService;

    /**
     * 사원(USER 계정) 등록 - ADMIN 전용
     * 회사는 요청 body가 아니라 토큰 기준(TenantContext)으로 고정 → 다른 회사에 사원을 넣을 수 없음
     */
    @PostMapping
    public ApiResponse<Void> saveEmployee(@Valid @RequestBody SignupRequest request) {
        long tenantId = TenantContext.get();
        authService.signup(request, tenantId, ErpAccountRole.USER);
        return ApiResponse.onSuccess(null);
    }

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
