// ItemServiceImpl.java
package erp.item.service;

import erp.global.exception.ErrorStatus;
import erp.global.exception.GlobalException;
import erp.global.response.PageResponse;
import erp.global.util.PageParam;
import erp.item.domain.Item;
import erp.item.dto.internal.ItemFindRow;
import erp.item.dto.internal.ItemOptionRow;
import erp.item.dto.internal.ItemPriceRow;
import erp.item.dto.request.ItemFindAllRequest;
import erp.item.dto.request.ItemSaveRequest;
import erp.item.dto.request.ItemUpdateRequest;
import erp.item.dto.response.ItemFindResponse;
import erp.item.dto.response.ItemInfoResponse;
import erp.item.dto.response.ItemOptionResponse;
import erp.item.enums.ItemCategory;
import erp.item.mapper.ItemMapper;
import erp.item.validation.ItemValidator;
import erp.log.audit.Auditable;
import erp.log.enums.LogType;
import erp.order.validation.OrderValidator;
import erp.purchase.validation.PurchaseValidator;
import erp.stock.service.StockService;
import erp.stock.validation.StockValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static erp.global.util.RowCountGuards.requireOneRowAffected;
import static erp.global.util.Strings.normalizeOrNull;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final StockService stockService;
    private final ItemValidator itemValidator;
    private final OrderValidator orderValidator;
    private final PurchaseValidator purchaseValidator;
    private final StockValidator stockValidator;

    @Auditable(type = LogType.WORK,
            messageEl = "'품목+재고 등록: name=' + #args[0].name() + ', code=' + #args[0].code() + ', qty=' + #args[0].initialQuantity()")
    @Override
    @Transactional
    public Long saveItemAndStock(ItemSaveRequest request, long tenantId) {
        String name = normalizeOrNull(request.name());
        String code = normalizeOrNull(request.code());
        ItemCategory category = request.category();

        itemValidator.validNameUnique(name, null, tenantId);
        itemValidator.validCodeUnique(code, null, tenantId);

        long newItemId = itemMapper.nextId();

        Item item = Item.register(
                newItemId,
                name,
                code,
                request.price(),
                request.unit(),
                category,
                tenantId
        );

        int affectedRowCount = itemMapper.save(tenantId, item);
        requireOneRowAffected(affectedRowCount, ErrorStatus.CREATE_ITEM_FAIL);

        stockService.saveStock(
                newItemId,
                request.initialQuantity(),
                request.warehouse(),
                tenantId
        );


        return newItemId;
    }

    @Override
    @Transactional(readOnly = true)
    public ItemInfoResponse findItem(long itemId, long tenantId) {
        Item item = itemMapper.findById(tenantId, itemId)
                .orElseThrow(() -> new GlobalException(ErrorStatus.NOT_FOUND_ITEM));
        return ItemInfoResponse.from(item);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ItemFindResponse> findAllItems(ItemFindAllRequest request, long tenantId) {
        PageParam pageParam = PageParam.of(request.page(), request.size(), 20);
        String name = normalizeOrNull(request.name());
        ItemCategory category = request.category(); // 검색 조건 없으면 null

        List<ItemFindRow> rows = itemMapper.findAllItemFindRow(
                tenantId, name, category, pageParam.offset(), pageParam.size());

        List<ItemFindResponse> responses = rows.stream()
                .map(ItemFindResponse::from)
                .toList();

        long total = itemMapper.countByNameAndCategory(tenantId, name, category);
        return PageResponse.of(responses, pageParam.page(), total, pageParam.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemOptionResponse> findAllItemOption(long tenantId) {
        List<ItemOptionRow> rows = itemMapper.findAllItemOptionRow(tenantId);
        return rows.stream()
                .map(ItemOptionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemPriceRow> findAllItemPriceByIds(List<Long> itemIds, long tenantId) {
        List<ItemPriceRow> priceRows = itemMapper.findAllPriceByIds(tenantId, itemIds);
        if (priceRows.isEmpty())
            throw new GlobalException(ErrorStatus.NOT_FOUND_ITEM_PRICE);
        return priceRows;
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'품목 수정: id=' + #args[0] + ', name=' + #args[1].name() + ', price=' + #args[1].price()")
    @Override
    @Transactional
    public void updateItem(long itemId, ItemUpdateRequest request, long tenantId) {
        String name = normalizeOrNull(request.name());
        ItemCategory category = request.category();

        itemValidator.validItemIdIfPresent(itemId, tenantId);
        itemValidator.validNameUnique(name, itemId, tenantId);

        Item item = Item.update(
                itemId,
                name,
                request.price(),
                request.unit(),
                category,
                tenantId
        );

        int affectedRowCount = itemMapper.updateById(tenantId, item);
        requireOneRowAffected(affectedRowCount, ErrorStatus.UPDATE_ITEM_FAIL);

        stockService.updateStockWarehouse(itemId, request.warehouse(), tenantId);
    }

    @Auditable(type = LogType.WORK,
            messageEl = "'품목 삭제(소프트): id=' + #args[0]")
    @Override
    @Transactional
    public void softDeleteItem(long itemId, long tenantId) {
        // 활성 품목인지 우선 확인
        itemValidator.validItemIdIfPresent(itemId, tenantId);
        // 재고 보유 시 삭제 불가
        stockValidator.validZeroStockByItemId(itemId, tenantId);
        // 진행 중 발주(CONFIRMED) 참조 시 삭제 불가
        purchaseValidator.validNoConfirmByItemId(itemId, tenantId);
        // 진행 중 주문(CONFIRMED) 참조 시 삭제 불가
        orderValidator.validNoConfirmByItemId(tenantId, itemId);

        int affectedRowCount = itemMapper.softDeleteById(tenantId, itemId);
        requireOneRowAffected(affectedRowCount, ErrorStatus.DELETE_ITEM_FAIL);
    }
}
