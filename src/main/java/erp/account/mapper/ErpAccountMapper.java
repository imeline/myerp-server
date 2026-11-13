package erp.account.mapper;

import erp.account.domain.ErpAccount;
import erp.account.dto.internal.LoginUserInfoRow;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import erp.account.dto.internal.AdminFindAllRow;
import java.util.List;
import erp.account.dto.internal.AdminFindRow;
import erp.account.dto.internal.AccountPasswordRow;

@Mapper
public interface ErpAccountMapper {

    long nextId();

    int save(ErpAccount account);

    Optional<Long> findCompanyIdByUuid(@Param("uuid") String uuid);

    Optional<LoginUserInfoRow> findLoginRowByLoginEmail(
            @Param("loginEmail") String loginEmail
    );

    int softDeleteByEmployeeId(@Param("tenantId") long tenantId,
            @Param("employeeId") long employeeId);

    boolean existsByLoginEmail(@Param("loginEmail") String loginEmail);

    // ===== 관리자(ADMIN) 계정 목록 - SYS 전용, 회사 구분 없이 전체 =====

    List<AdminFindAllRow> findAllAdminFindAllRow(@Param("companyName") String companyName,
                                                 @Param("name") String name,
                                                 @Param("offset") int offset,
                                                 @Param("size") int size);

    long countAdmin(@Param("companyName") String companyName,
                    @Param("name") String name);

    // ===== 비밀번호 변경·초기화 =====

    /** 로그인한 계정(uuid)의 계정 ID·비밀번호 해시 (삭제된 계정 제외) */
    Optional<AccountPasswordRow> findAccountPasswordRowByUuid(@Param("uuid") String uuid);

    /** 회사 안 사원의 계정 ID (삭제 계정·SYS 제외) - ADMIN의 사원 비밀번호 초기화용 */
    Optional<Long> findResettableErpAccountIdByEmployeeId(@Param("tenantId") long tenantId,
                                                          @Param("employeeId") long employeeId);

    int updatePasswordById(@Param("erpAccountId") long erpAccountId,
                           @Param("passwordHash") String passwordHash);

    /** ADMIN 역할이고 삭제되지 않은 계정 단건 */
    Optional<AdminFindRow> findAdminFindRowById(@Param("erpAccountId") long erpAccountId);
}
