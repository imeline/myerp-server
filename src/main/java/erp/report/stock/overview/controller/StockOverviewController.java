package erp.report.stock.overview.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.report.stock.overview.dto.request.StockOverviewFindRequest;
import erp.report.stock.overview.dto.response.StockOverviewResponse;
import erp.report.stock.overview.service.StockOverviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/report/stock/overview")
public class StockOverviewController {

    private final StockOverviewService stockOverviewService;

    @GetMapping
    public ApiResponse<StockOverviewResponse> getOverview(
            @Valid @ParameterObject @ModelAttribute StockOverviewFindRequest request
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(stockOverviewService.findStockOverview(request, tenantId));
    }
}
