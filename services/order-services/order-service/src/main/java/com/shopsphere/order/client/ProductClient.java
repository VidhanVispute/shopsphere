package com.shopsphere.order.client;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.order.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service", path = "/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable UUID id);
}