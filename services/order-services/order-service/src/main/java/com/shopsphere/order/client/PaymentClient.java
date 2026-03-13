package com.shopsphere.order.client;

import com.shopsphere.order.client.dto.InitiatePaymentRequest;
import com.shopsphere.order.client.dto.InitiatePaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/internal/payments/initiate")
    InitiatePaymentResponse initiatePayment(@RequestBody InitiatePaymentRequest request);
}