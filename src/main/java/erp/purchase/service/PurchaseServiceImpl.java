// PurchaseServiceImpl.java
package erp.purchase.service;

import erp.employee.service.EmployeeService;
import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.*;
import erp.global.util.time.DatePeriod;
import erp.global.util.time.DateRange;
import erp.global.util.time.Periods;
import erp.item.dto.internal.ItemPriceRow;
import erp.item.service.ItemService;
import erp.item.validation.ItemValidator;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import erp.purchase.domain.Purchase;
import erp.purchase.domain.PurchaseItem;
import erp.purchase.dto.internal.*;
import erp.purchase.dto.request.PurchaseFindAllRequest;
import erp.purchase.dto.request.PurchaseItemSaveRequest;
import erp.purchase.dto.request.PurchaseSaveRequest;
import erp.purchase.dto.response.*;
import erp.purchase.enums.PurchaseStatus;
import erp.purchase.mapper.PurchaseItemMapper;
import erp.purchase.mapper.PurchaseMapper;
import erp.purchase.validation.PurchaseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static erp.global.util.Codes.DEFAULT_MAX_TRY;
import static erp.global.util.RowCountGuards.requireOneRowAffected;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final PurchaseItemMapper purchaseItemMapper;

    private final ItemService itemService;

    private final EmployeeService employeeService;
    private final ItemValidator itemValidator;
    private final PurchaseValidator purchaseValidator;

    @Auditable(type = LogType.WORK,
            messageEl = "'발주 등록: supplier=' + #args[0].supplier() + ', items=' + #args[0].items().size()")
    @Override
    @Transactional
    public long savePurchaseAndPurchaseItems(PurchaseSaveRequest request, long tenantId, String accountUuid) {
        String supplier = Strings.normalizeOrNull(request.supplier());
        LocalDate purchaseDate = request.purchaseDate();
        long employeeId = employeeService.findActiveEmployeeIdByAccountUuid(accountUuid, tenantId); // 담당자 = 로그인한 본인 (재직 검증 포함)
        List<PurchaseItemSaveRequest> requestItems = request.items();

        // 1) 요청 DTO 내 itemId 중복 검사 (동일 품목 중복 방지)
        purchaseValidator.validItemIdsUniqueInRequest(requestItems);

        // 3) 아이템 단가 일괄 조회 (N+1 제거)
        List<Long> itemIds = requestItems.stream()
                .map(PurchaseItemSaveRequest::itemId)
                .toList();
        // 아이템 존재/활성 일괄 검증
        itemValidator.validItemIdsIfPresent(itemIds, tenantId);

        List<ItemPriceRow> priceRows = itemService.findAllItemPriceByIds(itemIds, tenantId);

        // 4) 조회 결과를 Map<Long itemId, Integer price>로 변환
        Map<Long, Integer> priceMap = priceRows.stream()
                .collect(Collectors.toMap(ItemPriceRow::itemId, ItemPriceRow::price));

        // 5) 합계 계산(총수량, 총금액)
        Totals totals = TotalCalculators.computeTotals(
                requestItems,
                PurchaseItemSaveRequest::quantity,
                PurchaseItemSaveRequest::itemId,
                priceMap);
        int totalQuantity = totals.totalQuantity();
        int totalAmount = totals.totalAmount();

        // 6) 발주 코드 생성 & 저장 (유니크 충돌 시 재시도)
        // MAX_TRY 인 5회 안에 성공하지 못할 확률은 극히 낮음(=사실상 불가능)
        int year = purchaseDate.getYear();
        for (int attempt = 0; attempt < DEFAULT_MAX_TRY; attempt++) {
            String purchaseCode = "PO-" + year + "-" + Codes.randomToken(8);
            // 발주코드 unique를 먼저 검사하고 저장을 하면 동시성 문제가 발생 가능
            // 저장 시도 후 DB에서 잡아주는 유니크 제약조건에 의존하는 것이 안전
            // → DuplicateKeyException 발생 시 재시도
            try {
                // 6-1) Purchase 저장
                long newPurchaseId = savePurchase(
                        purchaseCode,
                        supplier,
                        purchaseDate,
                        totalQuantity,
                        totalAmount,
                        employeeId,
                        tenantId
                );

                // 6-2) PurchaseItem 배치 저장
                savePurchaseItems(newPurchaseId, requestItems, tenantId);

                // 7) 성공: 생성된 purchase_id 반환
                return newPurchaseId;

            } catch (DuplicateKeyException e) {
                // 발주코드 유니크 충돌 시 재시도, 마지막 시도 실패 시 예외
                if (attempt == DEFAULT_MAX_TRY - 1) {
                    throw new GlobalException(ErrorStatus.CREATE_PURCHASE_FAIL);
                }
            }
        }

        // 8) 방어적 반환 (정상 흐름상 도달하지 않음)
        throw new GlobalException(ErrorStatus.CREATE_PURCHASE_FAIL);
    }

    // Purchase 저장
    private long savePurchase(String purchaseCode,
                              String supplier,
                              LocalDate purchaseDate,
                              int totalQuantity,
                              int totalAmount,
                              Long employeeId,
                              long tenantId) {
        long newPurchaseId = purchaseMapper.nextId();
        Purchase purchase = Purchase.register(
                newPurchaseId,
                purchaseCode,
                supplier,
                purchaseDate,
                totalQuantity,
                totalAmount,
                employeeId,
                tenantId
        );
        int affectedRowCount = purchaseMapper.save(tenantId, purchase);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_PURCHASE_FAIL);
        return newPurchaseId;
    }

    // PurchaseItem 배치 저장
    private void savePurchaseItems(long newPurchaseId,
                                   List<PurchaseItemSaveRequest> requestItems,
                                   long tenantId) {
        List<PurchaseItem> items = new ArrayList<>(requestItems.size());
        for (PurchaseItemSaveRequest itemSaveRequest : requestItems) {
            long newPurchaseItemId = purchaseItemMapper.nextId();
            items.add(PurchaseItem.register(
                    newPurchaseItemId, newPurchaseId,
                    itemSaveRequest.itemId(), itemSaveRequest.quantity(), tenantId
            ));
        }
        int affectedItems = purchaseItemMapper.saveAll(tenantId, items);
        if (affectedItems != items.size()) {
            throw new GlobalException(ErrorStatus.CREATE_PURCHASE_ITEM_FAIL);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseFindAllResponse> findAllPurchase(PurchaseFindAllRequest request,
                                                                 long tenantId) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);

        DatePeriod datePeriod = request.period();
        DateRange dateRange = Periods.resolve(datePeriod, LocalDate.now());
        LocalDate startDate = dateRange.startDate();
        LocalDate endDate = dateRange.endDate();
        String code = Strings.normalizeOrNull(request.code());
        PurchaseStatus status = request.status();

        // 4) 목록 조회 (삭제 제외, tenantGuard는 XML에서 WHERE 마지막)
        List<PurchaseFindRow> rows = purchaseMapper.findAllPurchaseFindRow(
                tenantId,
                startDate,
                endDate,
                code,
                status,
                pageParam.offset(),
                pageParam.size()
        );

        List<PurchaseFindAllResponse> responses = rows.stream()
                .map(PurchaseFindAllResponse::from)
                .toList();

        long total = purchaseMapper.countByPeriodAndCodeAndStatus(
                tenantId,
                startDate,
                endDate,
                code,
                status
        );

        return PageResponse.of(
                responses,
                pageParam.page(),
                total,
                pageParam.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseItemsSummaryResponse findPurchaseItemsSummary(long purchaseId, long tenantId) {
        purchaseValidator.validPurchaseIdIfPresent(purchaseId, tenantId);

        List<PurchaseItemStockFindRow> rows =
                purchaseItemMapper.findAllPurchaseItemStockFindRow(tenantId, purchaseId);
        if (rows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_PURCHASE_ITEM);
        }

        List<PurchaseItemStockFindResponse> items = rows.stream()
                .map(PurchaseItemStockFindResponse::from)
                .toList();

        return PurchaseItemsSummaryResponse.of(items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseCodeAndSupplierResponse> findAllPurchaseCodeAndSupplier(long tenantId) {
        List<PurchaseCodeAndSupplierRow> rows =
                purchaseMapper.findAllCodeAndSupplier(tenantId);

        return rows.stream()
                .map(PurchaseCodeAndSupplierResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseDetailResponse findPurchaseDetail(long purchaseId, long tenantId) {
        // 취소된 발주도 상세 조회 가능 (없으면 아래에서 NOT_FOUND)
        PurchaseDetailRow header = purchaseMapper.findPurchaseDetailRow(tenantId, purchaseId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_PURCHASE));

        List<PurchaseItemDetailRow> itemRows =
                purchaseItemMapper.findAllPurchaseItemDetailRow(tenantId, purchaseId);
        if (itemRows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_PURCHASE_ITEM);
        }

        return PurchaseDetailResponse.of(header, itemRows);
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'발주 취소: purchaseId=' + #args[0]")
    @Override
    @Transactional
    public void cancelPurchase(long purchaseId, long tenantId, String cancelerUuid) {
        // 1) 조건부 업데이트: CONFIRMED 상태일 때만 CANCELED로 변경 (SHIPPED는 불가)
        //    존재 검증을 먼저 하면 이미 취소된 건이 "없음"으로 나오므로, 실패했을 때 상태로 사유를 판정
        int affectedRowCount = purchaseMapper.updateStatusToCanceledIfConfirmed(tenantId,
                purchaseId, cancelerUuid);

        // 2) 성공 시 즉시 종료 (추가 조회 불필요)
        if (affectedRowCount == 1) {
            return;
        }

        // 3) 실패 사유 판정: 없음 / 입고됨 / 이미 취소됨
        PurchaseStatus status = purchaseMapper.findStatusById(tenantId, purchaseId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_PURCHASE));
        switch (status) {
            case SHIPPED -> throw new GlobalException(ErrorStatus.CANNOT_CANCEL_SHIPPED_PURCHASE);
            case CANCELED -> throw new GlobalException(ErrorStatus.ALREADY_CANCELED_PURCHASE);
            default -> throw new GlobalException(ErrorStatus.CANCEL_PURCHASE_FAIL);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseItemQuantityRow> findAllPurchaseItemQuantityRow(long purchaseId, long tenantId) {
        purchaseValidator.validPurchaseIdIfPresent(purchaseId, tenantId);

        List<PurchaseItemQuantityRow> rows =
                purchaseItemMapper.findAllPurchaseItemQuantityRow(tenantId, purchaseId);
        if (rows.isEmpty()) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_PURCHASE_ITEM);
        }
        return rows;
    }


    @Override
    @Transactional
    public void updateStatusToShippedIfConfirmed(long purchaseId, long tenantId) {

        int affectedRowCount = purchaseMapper.updateStatusToIfConfirmed(
                tenantId, purchaseId, PurchaseStatus.SHIPPED);
        if (affectedRowCount == 1) {
            return; // 성공
        }

        // 실패 사유 판정
        PurchaseStatus status = purchaseMapper.findStatusById(tenantId, purchaseId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_PURCHASE));

        switch (status) {
            case SHIPPED ->
                    throw new GlobalException(ErrorStatus.ALREADY_SHIPPED_PURCHASE);
            case CANCELED ->
                    throw new GlobalException(ErrorStatus.CANNOT_SHIP_CANCELED_PURCHASE);
        }
        throw new GlobalException(ErrorStatus.UPDATE_PURCHASE_STATUS_FAIL);
    }

    @Override
    @Transactional
    public void updateStatusToConfirmedIfShipped(long purchaseId, long tenantId) {
        int affectedRowCount = purchaseMapper.updateStatusToConfirmedIfShipped(tenantId, purchaseId);
        if (affectedRowCount == 1) {
            return;
        }

        // 실패 사유 판정
        PurchaseStatus status = purchaseMapper.findStatusById(tenantId, purchaseId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_PURCHASE));
        switch (status) {
            case CANCELED ->
                    throw new GlobalException(ErrorStatus.CANNOT_REVERT_CANCELED_PURCHASE);
            case CONFIRMED ->
                    throw new GlobalException(ErrorStatus.ALREADY_CONFIRMED_PURCHASE);
        }
        throw new GlobalException(ErrorStatus.UPDATE_PURCHASE_STATUS_FAIL);
    }
}
