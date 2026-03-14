package com.shopsphere.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent implements Serializable {

    public enum EventType {
        PRODUCT_CREATED,
        PRODUCT_UPDATED,
        PRODUCT_DELETED
    }

    private EventType eventType;
    private UUID productId;
    private String name;
    private String description;
    private BigDecimal price;
    private UUID categoryId;
    private String categoryName;
    private UUID vendorId;
    private String status;
    private String primaryImageUrl;
}
