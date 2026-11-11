package erp.outbound.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import erp.outbound.dto.request.OutboundFindAllRequest;
import erp.outbound.dto.request.OutboundSaveRequest;
import erp.outbound.dto.response.OutboundFindAllResponse;
import erp.outbound.service.OutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/outbound")
public class OutboundController {

    private final OutboundService outboundService;

    /**
     * 출고 등록 (출고 + 현재, 예약 재고 차감 + 주문 상태 전이)
     */
    @PostMapping
    public ApiResponse<Long> saveOutbound(@Valid @RequestBody OutboundSaveRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                outboundService.saveOutbound(request, tenantId));
    }

    /**
     * 출고 목록 조회 (기간/코드 검색 + 페이징, 취소 제외)
     */
    @GetMapping
    public ApiResponse<PageResponse<OutboundFindAllResponse>> findAllOutbound(
            @Valid @ParameterObject @ModelAttribute OutboundFindAllRequest request
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                outboundService.findAllOutbound(request, tenantId));
    }

    /**
     * 출고 취소 — status를 CANCELED로 변경. 재고 롤백 및 주문 상태 되돌림
     */
    @DeleteMapping("/{outboundId}")
    public ApiResponse<Void> cancelOutbound(@PathVariable long outboundId) {
        long tenantId = TenantContext.get();
        outboundService.cancelOutbound(outboundId, tenantId);
        return ApiResponse.onSuccess(null);
    }
}
