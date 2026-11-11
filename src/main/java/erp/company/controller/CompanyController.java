package erp.company.controller;

import erp.company.dto.request.CompanyFindAllRequest;
import erp.company.dto.request.CompanySaveRequest;
import erp.company.dto.request.CompanyUpdateRequest;
import erp.company.dto.response.CompanyFindResponse;
import erp.company.dto.response.CompanyInfoResponse;
import erp.company.service.CompanyService;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import erp.company.dto.response.CompanyOptionResponse;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sys/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ApiResponse<Long> saveCompany(@Valid @RequestBody CompanySaveRequest request) {
        return ApiResponse.onSuccess(companyService.saveCompany(request));
    }

    /**
     * 회사 선택 옵션 (관리자 계정 등록 화면 등)
     */
    @GetMapping("/options")
    public ApiResponse<List<CompanyOptionResponse>> findAllCompanyOption() {
        return ApiResponse.onSuccess(companyService.findAllCompanyOption());
    }

    @GetMapping("/{companyId}")
    public ApiResponse<CompanyInfoResponse> findCompany(@PathVariable Long companyId) {
        return ApiResponse.onSuccess(companyService.findCompany(companyId));
    }

    @GetMapping
    public ApiResponse<PageResponse<CompanyFindResponse>> findAllCompany(
            @Valid @ParameterObject @ModelAttribute CompanyFindAllRequest request
    ) {
        return ApiResponse.onSuccess(companyService.findAllCompany(request));
    }

    @PutMapping("/{companyId}")
    public ApiResponse<Void> updateCompany(@PathVariable Long companyId, @Valid @RequestBody CompanyUpdateRequest request) {
        companyService.updateCompany(companyId, request);
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/{companyId}")
    public ApiResponse<Void> softDeleteCompany(@PathVariable Long companyId) {
        companyService.softDeleteCompany(companyId);
        return ApiResponse.onSuccess(null);
    }
}
