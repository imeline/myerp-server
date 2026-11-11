package erp.inbound.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import erp.inbound.dto.request.InboundFindAllRequest;
import erp.inbound.dto.request.InboundSaveRequest;
import erp.inbound.dto.response.InboundFindAllResponse;
import erp.inbound.service.InboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import erp.auth.security.model.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/inbound")
public class InboundController {

    private final InboundService inboundService;

    @PostMapping
    public ApiResponse<Long> saveInbound(@Valid @RequestBody InboundSaveRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(inboundService.saveInbound(request, tenantId));
    }

    @GetMapping
    public ApiResponse<PageResponse<InboundFindAllResponse>> findAllInbound(
            @Valid @ParameterObject @ModelAttribute InboundFindAllRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(inboundService.findAllInbound(request, tenantId));
    }

    @DeleteMapping("/{inboundId}")
    public ApiResponse<Void> cancelInbound(@PathVariable("inboundId") long inboundId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        long tenantId = TenantContext.get();
        inboundService.cancelInbound(inboundId, tenantId, principal.getUuid());
        return ApiResponse.onSuccess(null);
    }
}
