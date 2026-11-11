package erp.outbound.mapper;

import erp.outbound.domain.Outbound;
import erp.outbound.dto.internal.OutboundCancelItemRow;
import erp.outbound.dto.internal.OutboundFindRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import erp.outbound.enums.OutboundStatus;
import java.util.Optional;

@Mapper
public interface OutboundMapper {

    long nextId();

    int save(@Param("tenantId") long tenantId,
             @Param("outbound") Outbound outbound);

    List<OutboundFindRow> findAllOutboundFindRow(
            @Param("tenantId") long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("code") String code,
            @Param("status") OutboundStatus status,
            @Param("offset") int offset,
            @Param("size") int size
    );

    List<OutboundCancelItemRow> findAllCancelItemRowById(
            @Param("tenantId") long tenantId,
            @Param("outboundId") long outboundId
    );

    int updateStatusToCanceledIfActive(
            @Param("tenantId") long tenantId,
            @Param("outboundId") long outboundId,
            @Param("cancelerUuid") String cancelerUuid
    );

    long countByPeriodAndCode(
            @Param("tenantId") long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("code") String code,
            @Param("status") OutboundStatus status
    );

    /** 상태 조회 (검증 실패 시 "없음"과 "이미 취소됨" 구분용) */
    Optional<OutboundStatus> findStatusById(@Param("tenantId") long tenantId,
                                  @Param("outboundId") long outboundId);

    boolean existsById(
            @Param("tenantId") long tenantId,
            @Param("outboundId") long outboundId
    );

}
