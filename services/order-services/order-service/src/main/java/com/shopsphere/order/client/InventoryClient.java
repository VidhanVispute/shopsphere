package com.shopsphere.order.client;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.order.client.dto.StockAdjustRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", path = "/internal/inventory")
public interface InventoryClient {

    @PostMapping("/reserve")
    ApiResponse<Void> reserve(@RequestBody StockAdjustRequest request);

    @PostMapping("/release")
    ApiResponse<Void> release(@RequestBody StockAdjustRequest request);
}