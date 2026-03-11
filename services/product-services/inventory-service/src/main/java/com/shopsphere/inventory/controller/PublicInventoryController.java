package com.shopsphere.inventory.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class PublicInventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(
                ApiResponse.success("Inventory fetched",
                        inventoryService.getInventory(productId)));
    }
}