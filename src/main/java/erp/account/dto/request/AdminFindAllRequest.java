package erp.account.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** 관리자(ADMIN) 계정 목록 검색 조건 - SYS 전용 */
public record AdminFindAllRequest(
        String companyName,  // 회사명 부분 검색
        String name,         // 관리자 이름 부분 검색
        @PositiveOrZero
        Integer page,
        @Positive            // null이면 PageParam 기본값 사용
        Integer size
) {
}
