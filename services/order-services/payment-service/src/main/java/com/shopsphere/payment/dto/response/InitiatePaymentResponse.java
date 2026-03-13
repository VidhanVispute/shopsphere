package com.shopsphere.payment.dto.response;

import com.shopsphere.payment.enums.PaymentMethod;
import com.shopsphere.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class InitiatePaymentResponse {
    private UUID paymentId;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    // Only populated for RAZORPAY — null for COD
    private String razorpayOrderId;
    private String razorpayKeyId;
}