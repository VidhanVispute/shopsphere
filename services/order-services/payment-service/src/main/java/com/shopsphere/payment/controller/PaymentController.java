package com.shopsphere.payment.controller;

import com.shopsphere.payment.dto.request.InitiatePaymentRequest;
import com.shopsphere.payment.dto.response.InitiatePaymentResponse;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Internal — called by OrderService Feign
    @PostMapping("/internal/payments/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiate(
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    // Razorpay webhook
    @PostMapping("/payments/webhook/razorpay")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    // User — fetch own payment by orderId
    @GetMapping("/payments/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }
}