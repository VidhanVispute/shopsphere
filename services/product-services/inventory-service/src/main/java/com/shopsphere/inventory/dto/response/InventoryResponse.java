package com.shopsphere.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryResponse {
    private UUID id;
    private UUID productId;
    private int availableQuantity;
    private int reservedQuantity;
    private Instant updatedAt;
}