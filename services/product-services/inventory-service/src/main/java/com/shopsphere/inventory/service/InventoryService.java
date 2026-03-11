package com.shopsphere.inventory.service;

import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.StockAdjustRequest;
import com.shopsphere.inventory.dto.request.StockUpdateRequest;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.entity.Inventory;
import com.shopsphere.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    // Admin — seed inventory for a new product
    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.findByProductId(request.getProductId()).isPresent()) {
            throw new IllegalArgumentException("Inventory already exists for product: " + request.getProductId());
        }
        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .availableQuantity(request.getInitialQuantity())
                .reservedQuantity(0)
                .build();
        return toResponse(inventoryRepository.save(inventory));
    }

    // Admin — add stock
    @Transactional
    public InventoryResponse addStock(UUID productId, StockUpdateRequest request) {
        Inventory inventory = getByProductId(productId);
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getQuantity());
        return toResponse(inventoryRepository.save(inventory));
    }

    // Public — check availability
    public InventoryResponse getInventory(UUID productId) {
        return toResponse(getByProductId(productId));
    }

    // Internal — called by OrderService via Feign when order is placed
    @Transactional
    public void reserve(StockAdjustRequest request) {
        Inventory inventory = getByProductId(request.getProductId());
        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Insufficient stock for product: " + request.getProductId());
        }
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
        inventoryRepository.save(inventory);
    }

    // Internal — called by OrderService via Feign when order is cancelled
    @Transactional
    public void release(StockAdjustRequest request) {
        Inventory inventory = getByProductId(request.getProductId());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - request.getQuantity());
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getQuantity());
        inventoryRepository.save(inventory);
    }

    private Inventory getByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productId));
    }

    private InventoryResponse toResponse(Inventory inv) {
        return InventoryResponse.builder()
                .id(inv.getId())
                .productId(inv.getProductId())
                .availableQuantity(inv.getAvailableQuantity())
                .reservedQuantity(inv.getReservedQuantity())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }
}