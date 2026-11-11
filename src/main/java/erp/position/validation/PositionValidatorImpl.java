package erp.position.validation;

import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.position.mapper.PositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionValidatorImpl implements PositionValidator {

    private final PositionMapper positionMapper;

    @Override
    public void validPositionIdIfPresent(Long positionId, long tenantId) {
        if (positionId == null) {return;} // 미지정이면 검사하지 않음 (DepartmentValidator와 같은 방식)
        if (positionMapper.findLevelNoById(tenantId, positionId).isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_POSITION);
        }
    }

    @Override
    public void validNameUnique(String name, Long excludeId, long tenantId) {
        if (name != null && positionMapper.existsByName(tenantId, name, excludeId)) {
            throw new GlobalException(ErrorStatus.DUPLICATE_POSITION_NAME);
        }
    }
}
