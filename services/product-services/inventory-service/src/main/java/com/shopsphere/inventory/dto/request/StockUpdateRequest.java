package com.shopsphere.inventory.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StockUpdateRequest {

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}