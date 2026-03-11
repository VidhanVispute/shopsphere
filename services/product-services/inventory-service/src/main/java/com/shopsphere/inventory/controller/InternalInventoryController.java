package com.shopsphere.inventory.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.inventory.dto.request.StockAdjustRequest;
import com.shopsphere.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryService inventoryService;

    // Called by OrderService Feign client on order placement
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<Void>> reserve(
            @Valid @RequestBody StockAdjustRequest request) {
        inventoryService.reserve(request);
        return ResponseEntity.ok(ApiResponse.success("Stock reserved", null));
    }

    // Called by OrderService Feign client on order cancellation
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> release(
            @Valid @RequestBody StockAdjustRequest request) {
        inventoryService.release(request);
        return ResponseEntity.ok(ApiResponse.success("Stock released", null));
    }
}