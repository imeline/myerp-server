package erp.outbound.dto.response;

import erp.outbound.dto.internal.OutboundFindRow;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import erp.outbound.enums.OutboundStatus;

@Builder
public record OutboundFindAllResponse(
        Long outboundId,
        String code,
        String customer,
        Integer itemCount,
        Integer totalAmount,
        LocalDate outboundDate,
        Long orderId,
        String orderCode,
        String employeeName,
        OutboundStatus status,
        LocalDateTime canceledAt,
        String canceledByName
) {
    public static OutboundFindAllResponse from(OutboundFindRow row) {
        return OutboundFindAllResponse.builder()
                .outboundId(row.outboundId())
                .code(row.code())
                .customer(row.customer())
                .itemCount(row.itemCount())
                .totalAmount(row.totalAmount())
                .outboundDate(row.outboundDate())
                .orderId(row.orderId())
                .orderCode(row.orderCode())
                .employeeName(row.employeeName())
                .status(row.status())
                .canceledAt(row.canceledAt())
                .canceledByName(row.canceledByName())
                .build();
    }
}
