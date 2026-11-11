// PositionServiceImpl.java
package erp.position.service;

import erp.employee.validation.EmployeeValidator;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import erp.position.domain.Position;
import erp.position.dto.request.PositionLevelNoRequest;
import erp.position.dto.request.PositionNameRequest;
import erp.position.dto.response.PositionFindAllResponse;
import erp.position.mapper.PositionMapper;
import erp.position.validation.PositionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static erp.global.util.RowCountGuards.requireOneRowAffected;
import static erp.global.util.Strings.normalizeOrNull;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

    private final PositionMapper positionMapper;
    private final PositionValidator positionValidator;
    private final EmployeeValidator employeeValidator;

    @Auditable(type = LogType.WORK,
            messageEl = "'직급 등록: name=' + #args[0].name()")
    @Override
    @Transactional
    public long savePosition(PositionNameRequest request, long tenantId) {
        long newPositionId = positionMapper.nextId();

        String name = normalizeOrNull(request.name());
        validNameUnique(name, null, tenantId);

        // 직급 번호는 가장 마지막 직급 번호 + 1
        int levelNo = positionMapper.findLastLevelNo(tenantId) + 1;

        Position position = Position.register(newPositionId, name, levelNo, tenantId);
        int affectedRowCount = positionMapper.save(tenantId, position);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_POSITION_FAIL);

        return newPositionId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionFindAllResponse> findAllPosition(long tenantId) {
        List<PositionFindAllResponse> list = positionMapper.findAll(tenantId).stream()
                .map(PositionFindAllResponse::from)
                .toList();
        return list;
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'직급명 변경: id=' + #args[0] + ' → ' + #args[1].name()")
    @Override
    @Transactional
    public void updatePositionName(long positionId, PositionNameRequest request, long tenantId) {
        String newName = request.name();
        validNameUnique(newName, positionId, tenantId);

        int affectedRowCount = positionMapper.updateNameById(tenantId, positionId, newName);
        requireOneRowAffected(affectedRowCount, ErrorStatus.UPDATE_POSITION_FAIL);
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'직급 레벨 변경: id=' + #args[0] + ', newLevel=' + #args[1].levelNo()")
    @Override
    @Transactional
    public void updatePositionLevelNo(long positionId, PositionLevelNoRequest request, long tenantId) {
        int newLevelNo = request.levelNo();
        int oldLevelNo = positionMapper.findLevelNoById(tenantId, positionId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_POSITION));

        if (newLevelNo < oldLevelNo)
            positionMapper.shiftUpRange(tenantId, newLevelNo, oldLevelNo);
        else if (newLevelNo > oldLevelNo)
            positionMapper.shiftDownRange(tenantId, oldLevelNo, newLevelNo);

        int affectedRowCount = positionMapper.updateLevelNoById(tenantId, positionId, newLevelNo);
        requireOneRowAffected(affectedRowCount, ErrorStatus.UPDATE_POSITION_FAIL);
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'직급 삭제: id=' + #args[0]")
    @Override
    @Transactional
    public void deletePosition(long positionId, long tenantId) {
        // 해당 직급에 속한 사원이 있는지 검사
        employeeValidator.validNoEmployeesInPosition(positionId, tenantId);
        // 삭제하려는 직급의 levelNo
        int oldLevelNo = positionMapper.findLevelNoById(tenantId, positionId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_POSITION));
        // 직급 삭제
        int affectedRowCount = positionMapper.deleteById(tenantId, positionId);
        requireOneRowAffected(affectedRowCount, ErrorStatus.DELETE_POSITION_FAIL);
        // 삭제된 직급보다 낮은 직급들의 levelNo -1
        positionMapper.shiftDownRange(tenantId, oldLevelNo, Integer.MAX_VALUE);
    }

    // 원래 네이밍/흐름 유지
    private void validNameUnique(String name, Long excludeId, long tenantId) {
        positionValidator.validNameUnique(name, excludeId, tenantId);
    }
}
