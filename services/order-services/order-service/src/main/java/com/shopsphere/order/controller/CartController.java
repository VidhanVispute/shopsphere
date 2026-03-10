package com.shopsphere.order.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.order.dto.request.AddToCartRequest;
import com.shopsphere.order.dto.request.UpdateCartRequest;
import com.shopsphere.order.dto.response.CartItemResponse;
import com.shopsphere.order.dto.response.CartResponse;
import com.shopsphere.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Cart fetched", cartService.getCart(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            Authentication auth,
            @Valid @RequestBody AddToCartRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Item added to cart",
                        cartService.addToCart(userId, request)));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateCartItem(
            Authentication auth,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Cart item updated",
                        cartService.updateCartItem(userId, productId, request)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            Authentication auth,
            @PathVariable UUID productId) {
        UUID userId = UUID.fromString(auth.getName());
        cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}