package com.shopsphere.order.client.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class InitiatePaymentResponse {
    private UUID paymentId;
    private String status;
    private String paymentMethod;
    private String razorpayOrderId;
    private String razorpayKeyId;
    
}
