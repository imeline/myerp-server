package erp.order.mapper;

import erp.order.domain.Order;
import erp.order.dto.internal.OrderCodeAndCustomerRow;
import erp.order.dto.internal.OrderDetailRow;
import erp.order.dto.internal.OrderFindRow;
import erp.order.enums.OrderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderMapper {
    long nextId();

    int save(@Param("tenantId") long tenantId, @Param("order") Order order);

    List<OrderFindRow> findAllOrderFindRow(
            @Param("tenantId") long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("code") String code,
            @Param("status") OrderStatus status,
            @Param("offset") int offset,
            @Param("size") int size
    );

    List<OrderCodeAndCustomerRow> findAllCodeAndCustomer(
            @Param("tenantId") long tenantId
    );

    Optional<OrderDetailRow> findOrderDetailRow(
            @Param("tenantId") long tenantId,
            @Param("orderId") long orderId
    );

    // 취소: CONFIRMED일 때만 CANCELED + 취소 일시·취소자 기록
    int updateStatusToCanceledIfConfirmed(
            @Param("tenantId") long tenantId,
            @Param("orderId") long orderId,
            @Param("cancelerUuid") String cancelerUuid
    );

    int updateStatusToIfConfirmed(
            @Param("tenantId") long tenantId,
            @Param("orderId") long orderId,
            @Param("toStatus") OrderStatus toStatus
    );

    int updateStatusToConfirmedIfShipped(
            @Param("tenantId") long tenantId,
            @Param("orderId") long orderId
    );

    Optional<OrderStatus> findStatusById(
            @Param("tenantId") long tenantId,
            @Param("orderId") long orderId
    );

    long countByPeriodAndCodeAndStatus(
            @Param("tenantId") long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("code") String code,
            @Param("status") OrderStatus status
    );

    boolean existsById(
            @Param("tenantId") long tenantId,
            @Param("orderId") long orderId
    );

    boolean existsConfirmByItemId(
            @Param("tenantId") long tenantId,
            @Param("orderId") long orderId
    );
}
