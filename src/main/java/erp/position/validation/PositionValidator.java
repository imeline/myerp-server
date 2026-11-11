package erp.position.validation;

public interface PositionValidator {
    void validPositionIdIfPresent(Long positionId, long tenantId);

    void validNameUnique(String name, Long excludeId, long tenantId);
}
