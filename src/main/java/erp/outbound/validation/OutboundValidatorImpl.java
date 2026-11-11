package erp.outbound.validation;

import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.outbound.mapper.OutboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import erp.outbound.enums.OutboundStatus;

@Component
@RequiredArgsConstructor
public class OutboundValidatorImpl implements OutboundValidator {

    private final OutboundMapper outboundMapper;

    @Override
    public void validOutboundIdIfPresent(long outboundId, long tenantId) {
        if (outboundMapper.existsById(tenantId, outboundId)) {
            return;
        }
        // 실패 사유 판정: 없음 / 이미 취소됨
        OutboundStatus status = outboundMapper.findStatusById(tenantId, outboundId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_OUTBOUND));
        if (status == OutboundStatus.CANCELED) {
            throw new GlobalException(ErrorStatus.ALREADY_CANCELED_OUTBOUND);
        }
        throw new GlobalException(ErrorStatus.NOT_FOUND_OUTBOUND);
    }
}
