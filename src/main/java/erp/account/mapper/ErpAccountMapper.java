package erp.account.mapper;

import erp.account.domain.ErpAccount;
import erp.account.dto.internal.LoginUserInfoRow;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import erp.account.dto.internal.AdminFindAllRow;
import java.util.List;
import erp.account.dto.internal.AdminFindRow;

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

    /** ADMIN 역할이고 삭제되지 않은 계정 단건 */
    Optional<AdminFindRow> findAdminFindRowById(@Param("erpAccountId") long erpAccountId);
}
