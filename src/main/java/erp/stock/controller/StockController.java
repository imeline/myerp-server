package erp.stock.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import erp.stock.dto.request.StockFindAllRequest;
import erp.stock.dto.request.StockMovementFindRequest;
import erp.stock.dto.response.StockFindAllResponse;
import erp.stock.dto.response.StockMovementFindAllResponse;
import erp.stock.dto.response.StockPriceFindResponse;
import erp.stock.dto.response.StockSummaryFindResponse;
import erp.stock.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/stock")
public class StockController {

    private final StockService stockService;

    /**
     * 재고 목록 조회
     */
    @GetMapping
    public ApiResponse<PageResponse<StockFindAllResponse>> findAllStocks(
            @Valid @ParameterObject @ModelAttribute StockFindAllRequest request
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                stockService.findAllStock(request, tenantId));
    }

    /**
     * 단일 품목 재고/단가 조회
     */
    @GetMapping("/{itemId}")
    public ApiResponse<StockPriceFindResponse> findStockPrice(
            @PathVariable long itemId
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(
                stockService.findStockPrice(itemId, tenantId));
    }

    /**
     * 단일 품목 재고 변동 내역 조회
     */
    @GetMapping("{itemId}/movement")
    public ApiResponse<StockMovementFindAllResponse> findMovements(
            @PathVariable long itemId,
            @Valid @ParameterObject @ModelAttribute StockMovementFindRequest request
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(stockService.findAllMovement(itemId, request, tenantId));
    }

    /**
     * 재고 현황 조회
     */
    @GetMapping("/summary")
    public ApiResponse<StockSummaryFindResponse> findSummary() {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(stockService.findSummary(tenantId));
    }

}
