package com.shopsphere.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PutMapping("/internal/orders/{orderId}/confirm")
    void confirmOrder(@PathVariable UUID orderId);
}