package erp.inbound.dto.response;

import erp.inbound.dto.internal.InboundFindAllRow;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import erp.inbound.enums.InboundStatus;

@Builder
public record InboundFindAllResponse(
        long inboundId,
        String inboundCode,
        LocalDate inboundDate,
        String employeeName,
        long purchaseId,
        String purchaseCode,
        int totalQuantity,
        int totalAmount,
        InboundStatus status,
        LocalDateTime canceledAt,
        String canceledByName
) {
    public static InboundFindAllResponse from(InboundFindAllRow row) {
        return InboundFindAllResponse.builder()
                .inboundId(row.inboundId())
                .inboundCode(row.inboundCode())
                .inboundDate(row.inboundDate())
                .employeeName(row.employeeName())
                .purchaseId(row.purchaseId())
                .purchaseCode(row.purchaseCode())
                .totalQuantity(row.totalQuantity())
                .totalAmount(row.totalAmount())
                .status(row.status())
                .canceledAt(row.canceledAt())
                .canceledByName(row.canceledByName())
                .build();
    }
}