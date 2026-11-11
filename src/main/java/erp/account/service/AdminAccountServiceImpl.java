package erp.account.service;

import erp.account.dto.internal.AdminFindRow;
import erp.account.dto.request.AdminFindAllRequest;
import erp.account.dto.request.AdminUpdateRequest;
import erp.account.dto.response.AdminFindAllResponse;
import erp.account.dto.response.AdminFindResponse;
import erp.account.mapper.ErpAccountMapper;
import erp.employee.service.EmployeeService;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.PageParam;
import erp.global.util.Strings;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements AdminAccountService {

    private final ErpAccountMapper erpAccountMapper;
    private final EmployeeService employeeService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminFindAllResponse> findAllAdmin(AdminFindAllRequest request) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);
        String companyName = Strings.normalizeOrNull(request.companyName());
        String name = Strings.normalizeOrNull(request.name());

        List<AdminFindAllResponse> responses = erpAccountMapper
                .findAllAdminFindAllRow(companyName, name, pageParam.offset(), pageParam.size())
                .stream()
                .map(AdminFindAllResponse::from)
                .toList();
        long total = erpAccountMapper.countAdmin(companyName, name);

        return PageResponse.of(responses, pageParam.page(), total, pageParam.size());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminFindResponse findAdmin(long erpAccountId) {
        return AdminFindResponse.from(findAdminRow(erpAccountId));
    }

    @Auditable(type = LogType.WORK, messageEl = "'관리자 수정: erpAccountId=' + #args[0] + ', name=' + #args[1].name()")
    @Override
    @Transactional
    public void updateAdmin(long erpAccountId, AdminUpdateRequest request) {
        AdminFindRow admin = findAdminRow(erpAccountId);
        // 사번·연락처 중복 검사는 그 관리자의 회사 안에서
        employeeService.updateEmployeeBasicInfo(
                admin.employeeId(), request.name(), request.empNo(), request.phone(), admin.companyId());
    }

    @Auditable(type = LogType.WORK, messageEl = "'관리자 삭제: erpAccountId=' + #args[0]")
    @Override
    @Transactional
    public void deleteAdmin(long erpAccountId) {
        AdminFindRow admin = findAdminRow(erpAccountId);
        employeeService.retireEmployee(admin.employeeId(), admin.companyId());
    }

    /** ADMIN 역할이고 삭제되지 않은 계정만 (없으면 ERP203) */
    private AdminFindRow findAdminRow(long erpAccountId) {
        return erpAccountMapper.findAdminFindRowById(erpAccountId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ADMIN));
    }
}
