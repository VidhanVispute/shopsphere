package com.shopsphere.product.dto.request;

import com.shopsphere.product.entity.Product;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateProductRequest {

    @Size(max = 255)
    private String name;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @Min(value = 0)
    private Integer stockQuantity;

    private UUID categoryId;

    private Product.Status status;

    private List<String> imageUrls;
}