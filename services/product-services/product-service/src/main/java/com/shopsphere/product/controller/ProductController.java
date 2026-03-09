package com.shopsphere.product.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.dto.response.ProductResponse;
import com.shopsphere.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ── Public ───────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<ProductResponse> products = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved", product));
    }

    // ── Vendor ───────────────────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<ProductResponse> products = productService.getMyProducts(
                UUID.fromString(userId), pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved", products));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.createProduct(
                UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse product = productService.updateProduct(
                id, UUID.fromString(userId), userRole, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        productService.deleteProduct(id, UUID.fromString(userId), userRole);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }
}