package erp.item.controller;

import erp.global.context.TenantContext;
import erp.global.response.ApiResponse;
import erp.global.response.PageResponse;
import erp.item.dto.request.ItemFindAllRequest;
import erp.item.dto.request.ItemSaveRequest;
import erp.item.dto.request.ItemUpdateRequest;
import erp.item.dto.response.ItemFindResponse;
import erp.item.dto.response.ItemInfoResponse;
import erp.item.dto.response.ItemOptionResponse;
import erp.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/item")
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ApiResponse<Long> saveItemAndStock(@Valid @RequestBody ItemSaveRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(itemService.saveItemAndStock(request, tenantId));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<ItemInfoResponse> findItem(@PathVariable Long itemId) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(itemService.findItem(itemId, tenantId));
    }

    @GetMapping
    public ApiResponse<PageResponse<ItemFindResponse>> findAllItem(
            @Valid @ParameterObject @ModelAttribute ItemFindAllRequest request) {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(itemService.findAllItems(request, tenantId));
    }

    @GetMapping("/options")
    public ApiResponse<List<ItemOptionResponse>> findAllItemOption() {
        long tenantId = TenantContext.get();
        return ApiResponse.onSuccess(itemService.findAllItemOption(tenantId));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<Void> updateItem(@PathVariable Long itemId,
                                        @Valid @RequestBody ItemUpdateRequest request) {
        long tenantId = TenantContext.get();
        itemService.updateItem(itemId, request, tenantId);
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> softDeleteItem(@PathVariable Long itemId) {
        long tenantId = TenantContext.get();
        itemService.softDeleteItem(itemId, tenantId);
        return ApiResponse.onSuccess(null);
    }
}
