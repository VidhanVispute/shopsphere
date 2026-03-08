package com.shopsphere.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fired by: Order Service
 * Consumed by: Inventory Service, Payment Service, Notification Service, Vendor Service
 *
 * WHY does this event carry so much data?
 * Each consumer needs different fields:
 *   Inventory needs: items (to reduce stock per SKU)
 *   Payment needs: orderId, totalAmount, userId (to initiate charge)
 *   Notification needs: userId, orderId (to send confirmation email)
 *   Vendor needs: vendorId, items (to alert vendor of new order)
 *
 * The event carries ALL of it. Each consumer picks what it needs.
 * This avoids consumers making synchronous Feign calls just to get
 * data they could have received in the event itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private String orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
    private List<OrderItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String skuId;
        private Long vendorId;
        private Integer quantity;
        private BigDecimal price;
    }
}