package erp.report.trade.overview.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.report.trade.overview.dto.request.TradeOverviewReportFindRequest;
import erp.report.trade.overview.dto.response.TradeOverviewReportResponse;
import erp.report.trade.overview.service.TradeOverviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/report/trade/overview")
public class TradeOverviewController {

    private final TradeOverviewService tradeOverviewService;

    @GetMapping
    public ApiResponse<TradeOverviewReportResponse> getOverview(
            @Valid @ParameterObject @ModelAttribute TradeOverviewReportFindRequest request
    ) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(tradeOverviewService.findTradeOverview(request, tenantId));
    }
}
