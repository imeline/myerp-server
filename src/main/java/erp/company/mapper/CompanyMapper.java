package erp.company.mapper;

import erp.company.domain.Company;
import erp.company.dto.internal.CompanyFindRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import erp.company.dto.internal.CompanyOptionRow;

@Mapper
public interface CompanyMapper {
    long nextId();

    int save(Company company);

    int updateById(Company company);

    Optional<Company> findById(@Param("companyId") Long companyId);

    // excludeId는 제외하고 검사(자기 자신 제외)
    boolean existsByBizNo(@Param("bizNo") String bizNo, @Param("excludeId") Long excludeId);

    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    List<CompanyFindRow> findAllCompanyRow(
            @Param("name") String name,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countByName(@Param("name") String name);

    /** 회사 선택 옵션 (삭제 제외, 이름순) */
    List<CompanyOptionRow> findAllCompanyOptionRow();

    int softDeleteById(@Param("companyId") Long companyId);

    boolean isActiveById(@Param("companyId") Long companyId);

    boolean existsCompanyById(@Param("companyId") Long companyId);

    boolean existsRelationData(@Param("companyId") Long companyId);
}
