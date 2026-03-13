package com.shopsphere.order.controller;

import com.shopsphere.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    // Called by PaymentService via Feign
    @PutMapping("/internal/orders/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(@PathVariable UUID orderId) {
        orderService.confirmOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
