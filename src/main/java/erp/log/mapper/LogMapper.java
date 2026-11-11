package erp.log.mapper;

import erp.log.domain.Log;
import erp.log.dto.internal.LogFindAllRow;
import erp.log.dto.internal.LogSearchFilter;
import erp.log.dto.internal.LogSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogMapper {

    long nextId();

    int save(@Param("tenantId") Long tenantId,
             @Param("log") Log log);

    int deleteOlderThan(@Param("type") String type,
                        @Param("days") int days);

    // ===== 시스템 로그 조회 (ADMIN, 자기 회사, ACCESS 제외) =====

    List<LogFindAllRow> findAllLogFindAllRow(@Param("tenantId") long tenantId,
                                       @Param("filter") LogSearchFilter filter,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    long countByFilters(@Param("tenantId") long tenantId,
                  @Param("filter") LogSearchFilter filter);

    LogSummaryRow findLogSummaryRow(@Param("tenantId") long tenantId);
}
