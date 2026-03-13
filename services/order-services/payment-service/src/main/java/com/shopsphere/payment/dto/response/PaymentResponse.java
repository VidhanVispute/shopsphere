package com.shopsphere.payment.dto.response;

import com.shopsphere.payment.enums.PaymentMethod;
import com.shopsphere.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime createdAt;
}