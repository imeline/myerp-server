package erp.inbound.validation;

import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.inbound.mapper.InboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import erp.inbound.enums.InboundStatus;

@Component
@RequiredArgsConstructor
public class InboundValidatorImpl implements InboundValidator {

    private final InboundMapper inboundMapper;

    @Override
    public void validInboundIdIfPresent(long inboundId, long tenantId) {
        if (!inboundMapper.existsById(tenantId, inboundId)) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_INBOUND);
        }
    }

    @Override
    public void validInboundActive(long inboundId, long tenantId) {
        if (inboundMapper.existsActiveById(tenantId, inboundId)) {
            return;
        }
        // 실패 사유 판정: 없음 / 이미 취소됨
        InboundStatus status = inboundMapper.findStatusById(tenantId, inboundId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_INBOUND));
        if (status == InboundStatus.CANCELED) {
            throw new GlobalException(ErrorStatus.ALREADY_CANCELED_INBOUND);
        }
        throw new GlobalException(ErrorStatus.INBOUND_NOT_ACTIVE);
    }
}
