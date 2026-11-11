package erp.stock.service;

import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.PageParam;
import erp.global.util.Strings;
import erp.global.util.time.DateRange;
import erp.global.util.time.Periods;
import erp.item.enums.ItemCategory;
import erp.item.validation.ItemValidator;
import erp.stock.domain.Stock;
import erp.stock.dto.internal.*;
import erp.stock.dto.request.StockFindAllRequest;
import erp.stock.dto.request.StockMovementFindRequest;
import erp.stock.dto.response.StockFindAllResponse;
import erp.stock.dto.response.StockMovementFindAllResponse;
import erp.stock.dto.response.StockPriceFindResponse;
import erp.stock.dto.response.StockSummaryFindResponse;
import erp.stock.enums.StockSortBy;
import erp.stock.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static erp.global.util.RowCountGuards.requireOneRowAffected;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;

    private final ItemValidator itemValidator;


    @Override
    @Transactional
    public long saveStock(long itemId, int initialQuantity, String warehouse,
                          long tenantId) {
        if (initialQuantity < 0)
            throw new GlobalException(ErrorStatus.INVALID_STOCK_QUANTITY);

        long newStockId = stockMapper.nextId();

        Stock stock = Stock.resister(
                newStockId,
                warehouse,
                initialQuantity,
                itemId,
                tenantId
        );

        int affectedRowCount = stockMapper.save(tenantId, stock);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_STOCK_FAIL);

        return newStockId;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockFindAllResponse> findAllStock(
            StockFindAllRequest request,
            long tenantId
    ) {
        String normalizedItemName = Strings.normalizeOrNull(request.name());
        String normalizedWarehouseName = Strings.normalizeOrNull(request.warehouse());
        ItemCategory itemCategory = request.category();

        PageParam pageParameter = PageParam.of(request.page(), request.size(), 20);

        StockSortBy sortBy = request.sortBy();
        String sortDirection = resolveDefaultSortDirection(sortBy);

        StockFindFilterRow filter = StockFindFilterRow.of(
                normalizedItemName,
                itemCategory,
                normalizedWarehouseName,
                request.availableQuantityFrom(),
                request.availableQuantityTo(),
                sortBy,
                sortDirection,
                pageParameter.offset(),
                pageParameter.size()
        );

        List<StockFindAllRow> rows = stockMapper.findAllStockFindAllRow(
                tenantId,
                filter
        );

        long totalCount = stockMapper.countByStock(
                tenantId,
                filter
        );
        List<StockFindAllResponse> responseList = rows.stream()
                .map(StockFindAllResponse::from)
                .toList();

        return PageResponse.of(
                responseList,
                pageParameter.page(),
                totalCount,
                pageParameter.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StockPriceFindResponse findStockPrice(long itemId, long tenantId) {
        StockPriceFindRow row =
                stockMapper.findStockPriceFindRowByItemId(tenantId, itemId)
                        .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ITEM));

        return StockPriceFindResponse.from(row);
    }

    @Override
    @Transactional
    public void updateStockWarehouse(long itemId, String warehouse, long tenantId) {
        String normalizedWarehouse = Strings.normalizeOrNull(warehouse);

        int affectedRowCount = stockMapper.updateWarehouse(
                tenantId, itemId, normalizedWarehouse);
        requireOneRowAffected(affectedRowCount, ErrorStatus.NOT_FOUND_STOCK);
    }

    @Override
    @Transactional
    public void increaseOnHand(long itemId, int delta, long tenantId) {
        if (delta <= 0) {
            throw new GlobalException(ErrorStatus.INVALID_STOCK_QUANTITY);
        }
        itemValidator.validItemIdIfPresent(itemId, tenantId);
        int affectedRowCount = stockMapper.increaseOnHand(
                tenantId, itemId, delta);
        requireOneRowAffected(affectedRowCount, ErrorStatus.NOT_FOUND_STOCK);
    }

    @Override
    @Transactional
    public void decreaseOnHand(long itemId, int delta, long tenantId) {
        if (delta <= 0) {
            throw new GlobalException(ErrorStatus.INVALID_STOCK_QUANTITY);
        }
        itemValidator.validItemIdIfPresent(itemId, tenantId);
        int affected = stockMapper.decreaseOnHandIfEnough(
                tenantId, itemId, delta);
        requireOneRowAffected(affected, ErrorStatus.NOT_FOUND_STOCK);
    }

    @Override
    @Transactional
    public void increaseAllocatedIfEnoughAvailable(long tenantId, long itemId, int quantity) {
        int affectedRowCount = stockMapper.increaseAllocatedIfEnoughOnHand(
                tenantId, itemId, quantity
        );
        requireOneRowAffected(affectedRowCount, ErrorStatus.INCREASE_STOCK_ALLOCATED_FAIL);
    }

    @Override
    @Transactional
    public void decreaseAllocated(long tenantId, long itemId, int quantity) {
        int affectedRowCount = stockMapper.decreaseAllocatedIfEnough(
                tenantId, itemId, quantity
        );
        requireOneRowAffected(affectedRowCount, ErrorStatus.DECREASE_STOCK_ALLOCATED_FAIL);
    }

    /**
     * 재고 변동 내역 조회 (팝업)
     * <p>
     * - 기간/상태/코드 필터 적용
     * - 요약(onHand)은 현재 재고 기준, 집계(입고/출고 건수)는 필터 기준
     */
    @Override
    @Transactional(readOnly = true)
    public StockMovementFindAllResponse findAllMovement(
            long itemId,
            StockMovementFindRequest request,
            long tenantId
    ) {
        DateRange dateRange = Periods.resolve(request.period(), LocalDate.now());
        String normalizedCode = Strings.normalizeOrNull(request.code());
        PageParam pageParameter = PageParam.of(request.page(), request.size(), 20);

        StockMovementSearchFilter filter = StockMovementSearchFilter.of(
                itemId,
                dateRange.startDate(),
                dateRange.endDate(),
                normalizedCode,
                request.status(),
                pageParameter
        );

        StockMovementSummaryRow summaryFromDatabase =
                stockMapper.findMovementSummary(tenantId, filter);
        // 해당 테넌트에 없는 품목이면 요약 행이 null → NPE(500) 대신 NOT_FOUND_ITEM
        if (summaryFromDatabase == null) {
            throw new GlobalException(ErrorStatus.NOT_FOUND_ITEM);
        }
        StockMovementSummaryRow summary = StockMovementSummaryRow.from(summaryFromDatabase);


        List<StockMovementFindRow> rows = stockMapper.findMovementRows(tenantId, filter);
        long totalCount = stockMapper.countMovementRows(tenantId, filter);

        return StockMovementFindAllResponse.of(
                summary,
                rows,
                filter.page(),
                totalCount,
                filter.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StockSummaryFindResponse findSummary(long tenantId) {
        StockSummaryRow row = stockMapper.findStockSummaryRow(tenantId);
        return StockSummaryFindResponse.from(row);
    }


    /**
     * 기본 정렬 방향 정책
     * - 텍스트(ITEM_NAME, ITEM_CODE): ASC
     * - 수치/일자(그 외): DESC
     */
    private String resolveDefaultSortDirection(
            StockSortBy sortBy
    ) {
        if (sortBy == null) {
            return "DESC";
        }
        return switch (sortBy) {
            case ITEM_NAME, ITEM_CODE -> "ASC";
            default -> "DESC";
        };
    }
}
