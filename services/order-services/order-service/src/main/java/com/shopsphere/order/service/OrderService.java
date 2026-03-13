package com.shopsphere.order.service;

import com.shopsphere.common.dto.NotificationEvent;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.order.client.InventoryClient;
import com.shopsphere.order.client.PaymentClient;
import com.shopsphere.order.client.ProductClient;
import com.shopsphere.order.client.dto.InitiatePaymentRequest;
import com.shopsphere.order.client.dto.ProductResponse;
import com.shopsphere.order.client.dto.StockAdjustRequest;
import com.shopsphere.order.dto.request.PlaceOrderRequest;
import com.shopsphere.order.dto.response.OrderItemResponse;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.entity.CartItem;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.order.messaging.NotificationPublisher;
import com.shopsphere.order.repository.CartItemRepository;
import com.shopsphere.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public OrderResponse placeOrder(UUID userId, String userEmail, PlaceOrderRequest request) {
        // 1. Fetch cart items
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // 2. Filter by selected products if provided
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            cartItems = cartItems.stream()
                    .filter(item -> request.getProductIds().contains(item.getProductId()))
                    .toList();
            if (cartItems.isEmpty()) {
                throw new BadRequestException("None of the selected products are in your cart");
            }
        }

        // 3. Validate each product and build order items
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            ProductResponse product = fetchProduct(cartItem.getProductId());

            if (!"ACTIVE".equals(product.getStatus())) {
                throw new BadRequestException("Product is no longer available: " + product.getName());
            }

            reserveStock(cartItem.getProductId(), cartItem.getQuantity(), product.getName());

            return OrderItem.builder()
                    .productId(product.getId())
                    .vendorId(product.getVendorId())
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
        }).toList();

        // 4. Calculate total
        BigDecimal total = orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Build and save order
        Order order = Order.builder()
                .userId(userId)
                .userEmail(userEmail)
                .status("PENDING")
                .totalAmount(total)
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        orderItems.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order savedOrder = orderRepository.save(order);

        // 6. Clear ordered items from cart
        List<UUID> orderedProductIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();
        orderedProductIds.forEach(productId ->
                cartItemRepository.deleteByUserIdAndProductId(userId, productId));

        // 7. Initiate payment + publish notification AFTER transaction commits.
        //
        //    WHY afterCommit():
        //    For COD, PaymentService immediately calls back confirmOrder() in the same
        //    thread cycle. If called inside this @Transactional method, the order row
        //    is NOT yet committed, so confirmOrder() gets 404. afterCommit() guarantees
        //    the row is visible before PaymentService reads it.

        final UUID orderId     = savedOrder.getId();
        final UUID orderUserId = savedOrder.getUserId();
        final BigDecimal amount = savedOrder.getTotalAmount();
        final String method    = savedOrder.getPaymentMethod();
        final String email     = savedOrder.getUserEmail();

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Payment initiation
                    try {
                        paymentClient.initiatePayment(
                                new InitiatePaymentRequest(orderId, orderUserId, amount, method));
                        log.info("Payment initiated for order {}", orderId);
                    } catch (Exception e) {
                        log.error("Payment initiation failed for order {}: {}",
                                orderId, e.getMessage());
                    }

                    // Notification — fire and forget, never blocks order flow
                    notificationPublisher.publishOrderPlaced(
                            NotificationEvent.builder()
                                    .eventType(NotificationEvent.EventType.ORDER_PLACED)
                                    .orderId(orderId)
                                    .userId(orderUserId)
                                    .userEmail(email)
                                    .orderAmount("₹" + amount)
                                    .paymentMethod(method)
                                    .build());
                }
            }
        );

        log.info("Order {} placed for user {}", savedOrder.getId(), userId);
        return toOrderResponse(savedOrder);
    }

    // ── Called by PaymentService via Feign ────────────────────────────────────

    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        order.setStatus("CONFIRMED");
        orderRepository.save(order);

        // Publish ORDER_CONFIRMED notification
        notificationPublisher.publishOrderConfirmed(
                NotificationEvent.builder()
                        .eventType(NotificationEvent.EventType.ORDER_CONFIRMED)
                        .orderId(orderId)
                        .userId(order.getUserId())
                        .userEmail(order.getUserEmail())
                        .orderAmount("₹" + order.getTotalAmount())
                        .paymentMethod(order.getPaymentMethod())
                        .build());

        log.info("Order {} confirmed by PaymentService", orderId);
    }

    // ── User queries ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!"PENDING".equals(order.getStatus())) {
            throw new BadRequestException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        Order saved = orderRepository.save(order);

        saved.getItems().forEach(item -> releaseStock(item.getProductId(), item.getQuantity()));

        // Publish ORDER_CANCELLED notification
        notificationPublisher.publishOrderCancelled(
                NotificationEvent.builder()
                        .eventType(NotificationEvent.EventType.ORDER_CANCELLED)
                        .orderId(orderId)
                        .userId(order.getUserId())
                        .userEmail(order.getUserEmail())
                        .orderAmount("₹" + order.getTotalAmount())
                        .paymentMethod(order.getPaymentMethod())
                        .build());

        log.info("Order {} cancelled by user {}", orderId, userId);
        return toOrderResponse(saved);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toOrderResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        validateStatusTransition(order.getStatus(), status);
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, status);
        return toOrderResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateStatusTransition(String current, String next) {
        boolean valid = switch (current) {
            case "PENDING"   -> List.of("CONFIRMED", "CANCELLED").contains(next);
            case "CONFIRMED" -> List.of("SHIPPED", "CANCELLED").contains(next);
            case "SHIPPED"   -> List.of("DELIVERED").contains(next);
            default          -> false;
        };
        if (!valid) {
            throw new BadRequestException("Invalid status transition: " + current + " → " + next);
        }
    }

    private ProductResponse fetchProduct(UUID productId) {
        try {
            var response = productClient.getProductById(productId);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }
            return response.getData();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch product {}: {}", productId, e.getMessage());
            throw new BadRequestException("Unable to reach product service. Please try again.");
        }
    }

    private void reserveStock(UUID productId, int quantity, String productName) {
        try {
            inventoryClient.reserve(new StockAdjustRequest(productId, quantity));
        } catch (Exception e) {
            log.error("Failed to reserve stock for product {}: {}", productId, e.getMessage());
            throw new BadRequestException("Insufficient stock for: " + productName);
        }
    }

    private void releaseStock(UUID productId, int quantity) {
        try {
            inventoryClient.release(new StockAdjustRequest(productId, quantity));
        } catch (Exception e) {
            log.error("Failed to release stock for product {}: {}", productId, e.getMessage());
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .orderId(item.getOrder().getId())
                        .productId(item.getProductId())
                        .vendorId(item.getVendorId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentId(order.getPaymentId())
                .notes(order.getNotes())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}