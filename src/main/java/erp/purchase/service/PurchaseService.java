package erp.purchase.service;

import erp.global.response.PageResponse;
import erp.purchase.dto.internal.PurchaseItemQuantityRow;
import erp.purchase.dto.request.PurchaseFindAllRequest;
import erp.purchase.dto.request.PurchaseSaveRequest;
import erp.purchase.dto.response.PurchaseCodeAndSupplierResponse;
import erp.purchase.dto.response.PurchaseDetailResponse;
import erp.purchase.dto.response.PurchaseFindAllResponse;
import erp.purchase.dto.response.PurchaseItemsSummaryResponse;

import java.util.List;

public interface PurchaseService {

    long savePurchaseAndPurchaseItems(
            PurchaseSaveRequest request, long tenantId, String accountUuid);

    PageResponse<PurchaseFindAllResponse> findAllPurchase(
            PurchaseFindAllRequest request, long tenantId);

    PurchaseItemsSummaryResponse findPurchaseItemsSummary(long purchaseId, long tenantId);

    List<PurchaseCodeAndSupplierResponse> findAllPurchaseCodeAndSupplier(
            long tenantId);

    PurchaseDetailResponse findPurchaseDetail(long purchaseId, long tenantId);

    void cancelPurchase(long purchaseId, long tenantId, String cancelerUuid);

    List<PurchaseItemQuantityRow> findAllPurchaseItemQuantityRow(long purchaseId, long tenantId);

    void updateStatusToShippedIfConfirmed(long purchaseId, long tenantId);

    void updateStatusToConfirmedIfShipped(long purchaseId, long tenantId);
}
