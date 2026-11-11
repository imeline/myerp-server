// CompanyServiceImpl.java
package erp.company.service;

import erp.company.domain.Company;
import erp.company.dto.internal.CompanyFindRow;
import erp.company.dto.request.CompanyFindAllRequest;
import erp.company.dto.request.CompanySaveRequest;
import erp.company.dto.request.CompanyUpdateRequest;
import erp.company.dto.response.CompanyFindResponse;
import erp.company.dto.response.CompanyInfoResponse;
import erp.company.mapper.CompanyMapper;
import erp.company.validation.CompanyValidator;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.PageParam;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static erp.global.util.RowCountGuards.requireOneRowAffected;
import static erp.global.util.Strings.normalizeOrNull;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyMapper companyMapper;
    private final CompanyValidator companyValidator;

    @Auditable(type = LogType.WORK, messageEl = "'회사 등록: ' + #args[0].name() + ' (' + #args[0].bizNo() + ')' ")
    @Override
    @Transactional
    public Long saveCompany(CompanySaveRequest request) {
        companyValidator.validBizNoUnique(request.bizNo(), null);
        companyValidator.validNameUnique(request.name(), null);

        long newId = companyMapper.nextId();
        Company company = Company.of(
                newId,
                request.name(),
                request.bizNo(),
                request.address(),
                request.phone()
        );

        int affectedRowCount = companyMapper.save(company);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_COMPANY_FAIL);
        return newId;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyInfoResponse findCompany(long companyId) {
        Company company = companyMapper.findById(companyId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_COMPANY));
        return CompanyInfoResponse.from(company);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CompanyFindResponse> findAllCompany(CompanyFindAllRequest request) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);
        String name = normalizeOrNull(request.name());

        List<CompanyFindRow> rows = companyMapper.findAllCompanyRow(name, pageParam.offset(), pageParam.size());

        List<CompanyFindResponse> responses = rows.stream()
                .map(CompanyFindResponse::from)
                .toList();

        long total = companyMapper.countByName(name);
        return PageResponse.of(responses, pageParam.page(), total, pageParam.size());
    }

    @Auditable(type = LogType.WORK, messageEl = "'회사 수정: id=' + #args[0] + ', name=' + #args[1].name() + ', bizNo=' + #args[1].bizNo()")
    @Override
    @Transactional
    public void updateCompany(Long companyId, CompanyUpdateRequest request) {
        companyValidator.validCompanyIdIfPresent(companyId);
        // 중복 여부 체크
        companyValidator.validBizNoUnique(request.bizNo(), companyId);
        companyValidator.validNameUnique(request.name(), companyId);

        Company company = Company.of(
                companyId,
                request.name(),
                request.bizNo(),
                request.address(),
                request.phone()
        );

        int affectedRowCount = companyMapper.updateById(company);
        requireOneRowAffected(affectedRowCount, ErrorStatus.UPDATE_COMPANY_FAIL);
    }

    @Auditable(type = LogType.WORK, messageEl = "'회사 삭제(소프트): id=' + #args[0]")
    @Override
    @Transactional
    public void softDeleteCompany(long companyId) {
        if (companyMapper.existsRelationData(companyId)) {
            throw new GlobalException(ErrorStatus.CANNOT_DELETE_COMPANY_HAS_RELATION);
        }
        int affectedRowCount = companyMapper.softDeleteById(companyId);
        requireOneRowAffected(affectedRowCount, ErrorStatus.DELETE_COMPANY_FAIL);
    }
}
