package com.shopsphere.product.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopsphere.product.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class ProductResponse {

    @JsonProperty("id")
    private UUID productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Product.Status status;
    private UUID vendorId;
    private UUID categoryId;
    private String categoryName;
    private List<String> imageUrls;
    private String primaryImageUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}