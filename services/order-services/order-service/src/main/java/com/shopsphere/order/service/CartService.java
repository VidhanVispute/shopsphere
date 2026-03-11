package com.shopsphere.order.service;

import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.order.client.ProductClient;
import com.shopsphere.order.client.dto.ProductResponse;
import com.shopsphere.order.dto.request.AddToCartRequest;
import com.shopsphere.order.dto.request.UpdateCartRequest;
import com.shopsphere.order.dto.response.CartItemResponse;
import com.shopsphere.order.dto.response.CartResponse;
import com.shopsphere.order.entity.CartItem;
import com.shopsphere.order.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    @Transactional
    public CartItemResponse addToCart(UUID userId, AddToCartRequest request) {
        // 1. Fetch product from Product Service via Feign
        ProductResponse product = getProduct(request.getId());

        // 2. Validate product is available
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new com.shopsphere.common.exception.BadRequestException(
                "Product is not available for purchase"
            );
        }
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new com.shopsphere.common.exception.BadRequestException(
                "Insufficient stock. Available: " + product.getStockQuantity()
            );
        }

        // 3. If already in cart — update quantity
        var existing = cartItemRepository
                .findByUserIdAndProductId(userId, request.getId());

        CartItem cartItem;
        if (existing.isPresent()) {
            cartItem = existing.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItem.setProductPrice(product.getPrice()); // refresh price
        } else {
            cartItem = CartItem.builder()
                    .userId(userId)
                    .productId(product.getId())
                    .vendorId(product.getVendorId())
                    .productName(product.getName())
                    .productPrice(product.getPrice())
                    .quantity(request.getQuantity())
                    .build();
        }

        cartItem = cartItemRepository.save(cartItem);
        log.info("Cart updated for user {} — product {}", userId, request.getId());
        return toCartItemResponse(cartItem);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> responses = items.stream()
                .map(this::toCartItemResponse)
                .toList();

        BigDecimal total = responses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(responses)
                .totalItems(responses.size())
                .totalAmount(total)
                .build();
    }

    @Transactional
    public CartItemResponse updateCartItem(UUID userId, UUID productId,
                                           UpdateCartRequest request) {
        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found for product: " + productId));

        // Refresh price from product service
        ProductResponse product = getProduct(productId);
        cartItem.setQuantity(request.getQuantity());
        cartItem.setProductPrice(product.getPrice());

        cartItem = cartItemRepository.save(cartItem);
        return toCartItemResponse(cartItem);
    }

    @Transactional
    public void removeFromCart(UUID userId, UUID productId) {
        if (!cartItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException(
                    "Cart item not found for product: " + productId);
        }
        cartItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void clearCart(UUID userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ProductResponse getProduct(UUID productId) {
        try {
            var response = productClient.getProductById(productId);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }
            return response.getData();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch product {} from product-service: {}",
                    productId, e.getMessage());
            throw new com.shopsphere.common.exception.BadRequestException(
                    "Unable to reach product service. Please try again.");
        }
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .vendorId(item.getVendorId())
                .productName(item.getProductName())
                .productPrice(item.getProductPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getProductPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}