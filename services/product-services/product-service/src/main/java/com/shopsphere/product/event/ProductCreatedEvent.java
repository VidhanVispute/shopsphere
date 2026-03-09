package com.shopsphere.product.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    private UUID productId;
    private String name;
    private String description;
    private BigDecimal price;
    private UUID categoryId;
    private String categoryName;
    private UUID vendorId;
    private String primaryImageUrl;
}