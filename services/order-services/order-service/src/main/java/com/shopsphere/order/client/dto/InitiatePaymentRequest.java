package com.shopsphere.order.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data 
@AllArgsConstructor 
@NoArgsConstructor
public class InitiatePaymentRequest {
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String paymentMethod; // "COD" or "RAZORPAY"
}
    

