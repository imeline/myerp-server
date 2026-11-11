package erp.purchase.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import erp.purchase.dto.request.PurchaseFindAllRequest;
import erp.purchase.dto.request.PurchaseSaveRequest;
import erp.purchase.dto.response.PurchaseCodeAndSupplierResponse;
import erp.purchase.dto.response.PurchaseDetailResponse;
import erp.purchase.dto.response.PurchaseFindAllResponse;
import erp.purchase.dto.response.PurchaseItemsSummaryResponse;
import erp.purchase.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import erp.auth.security.model.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * 발주 등록 (발주 + 발주아이템 저장)
     */
    @PostMapping
    public ApiResponse<Long> savePurchaseAndItems(@Valid @RequestBody PurchaseSaveRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                purchaseService.savePurchaseAndPurchaseItems(request, tenantId));
    }

    /**
     * 발주 목록 조회 (기간/상태/코드 검색 + 페이징, 삭제 제외)
     */
    @GetMapping
    public ApiResponse<PageResponse<PurchaseFindAllResponse>> findAllPurchases(
            @Valid @ParameterObject @ModelAttribute PurchaseFindAllRequest request
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                purchaseService.findAllPurchase(request, tenantId));
    }

    /**
     * 특정 발주의 발주아이템 목록 요약 조회
     */
    @GetMapping("/{purchaseId}/items/summary")
    public ApiResponse<PurchaseItemsSummaryResponse> findPurchaseItemsSummary(
            @PathVariable long purchaseId) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                purchaseService.findPurchaseItemsSummary(purchaseId, tenantId));
    }

    /**
     * 발주번호·공급사 목록 (CONFIRMED만)
     */
    @GetMapping("/code-suppliers")
    public ApiResponse<List<PurchaseCodeAndSupplierResponse>> findAllPurchaseCodeAndSupplier() {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                purchaseService.findAllPurchaseCodeAndSupplier(tenantId));
    }

    /**
     * 발주 상세 조회
     */
    @GetMapping("/{purchaseId}")
    public ApiResponse<PurchaseDetailResponse> findPurchaseDetail(@PathVariable Long purchaseId) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                purchaseService.findPurchaseDetail(purchaseId, tenantId));
    }

    /**
     * 발주 취소(삭제) — status를 CANCELED로 변경. SHIPPED이면 불가
     */
    @DeleteMapping("/{purchaseId}")
    public ApiResponse<Void> cancelPurchase(@PathVariable Long purchaseId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        long tenantId = TenantContext.get();
        purchaseService.cancelPurchase(purchaseId, tenantId, principal.getUuid());
        return ApiResponse.onSuccess(null);
    }
}
