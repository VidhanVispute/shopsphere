package com.shopsphere.inventory.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.StockUpdateRequest;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory created",
                        inventoryService.createInventory(request)));
    }

    @PutMapping("/{productId}/add-stock")
    public ResponseEntity<ApiResponse<InventoryResponse>> addStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Stock updated",
                        inventoryService.addStock(productId, request)));
    }
}