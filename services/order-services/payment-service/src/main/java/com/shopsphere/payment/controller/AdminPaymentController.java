package com.shopsphere.payment.controller;

import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/admin/payments")
    public ResponseEntity<List<PaymentResponse>> getAll() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}