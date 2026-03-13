package com.shopsphere.order.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.order.dto.request.PlaceOrderRequest;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            Authentication auth,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody PlaceOrderRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully",
                        orderService.placeOrder(userId, userEmail, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            Authentication auth,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Orders fetched",
                        orderService.getMyOrders(userId, pageable)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            Authentication auth,
            @PathVariable UUID orderId) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Order fetched",
                        orderService.getOrderById(orderId, userId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            Authentication auth,
            @PathVariable UUID orderId) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Order cancelled",
                        orderService.cancelOrder(orderId, userId)));
    }
}