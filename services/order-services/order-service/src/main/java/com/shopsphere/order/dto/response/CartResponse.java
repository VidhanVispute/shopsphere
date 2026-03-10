package com.shopsphere.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {

    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalAmount;
}