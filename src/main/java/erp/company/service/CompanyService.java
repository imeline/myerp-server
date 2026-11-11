package erp.company.service;

import erp.company.dto.request.CompanyFindAllRequest;
import erp.company.dto.request.CompanySaveRequest;
import erp.company.dto.request.CompanyUpdateRequest;
import erp.company.dto.response.CompanyFindResponse;
import erp.company.dto.response.CompanyInfoResponse;
import erp.global.response.PageResponse;
import erp.company.dto.response.CompanyOptionResponse;
import java.util.List;

public interface CompanyService {
    Long saveCompany(CompanySaveRequest request);

    CompanyInfoResponse findCompany(long companyId);

    PageResponse<CompanyFindResponse> findAllCompany(CompanyFindAllRequest request);

    void updateCompany(Long companyId, CompanyUpdateRequest request);

    void softDeleteCompany(long companyId);


    /** 회사 선택 옵션 (삭제 제외, 이름순) */
    List<CompanyOptionResponse> findAllCompanyOption();
}
