package com.shopsphere.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    public enum EventType {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        ORDER_CANCELLED
    }

    private EventType eventType;
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private String userName;
    private String orderAmount;   // formatted string e.g. "₹129,999.00"
    private String paymentMethod; // "COD" or "RAZORPAY"
}