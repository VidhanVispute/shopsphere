package com.shopsphere.product.service;

import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.dto.response.ProductResponse;
import java.util.concurrent.CompletableFuture;
import com.shopsphere.product.entity.Category;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.product.event.ProductCreatedEvent;
import com.shopsphere.product.repository.CategoryRepository;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository        productRepository;
    private final CategoryRepository       categoryRepository;
    private final ProductImageRepository   productImageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Public endpoints ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Pageable pageable) {
        return productRepository
                .findByStatus(Product.Status.ACTIVE, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = findActiveById(id);
        return toResponse(product);
    }

    // ── Vendor endpoints ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(UUID vendorId, Pageable pageable) {
        return productRepository
                .findByVendorIdAndStatusNot(vendorId, Product.Status.DELETED, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ProductResponse createProduct(UUID vendorId, CreateProductRequest request) {
        Category category = resolveCategory(request.getCategoryId());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .vendorId(vendorId)
                .category(category)
                .status(Product.Status.ACTIVE)
                .build();

        // Attach images
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<ProductImage> images = buildImages(request.getImageUrls(), product);
            product.getImages().addAll(images);
        }

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, vendorId={}", saved.getId(), vendorId);

        // Publish Kafka event — fire and forget
        publishProductCreatedEvent(saved);

        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, UUID requesterId,
                                         String requesterRole, UpdateProductRequest request) {
        Product product = findById(productId);

        // Vendor can only update their own; ADMIN can update any
        if ("VENDOR".equals(requesterRole) && !product.getVendorId().equals(requesterId)) {
            throw new BadRequestException("You do not own this product");
        }

        if (request.getName() != null)          product.setName(request.getName());
        if (request.getDescription() != null)   product.setDescription(request.getDescription());
        if (request.getPrice() != null)         product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getStatus() != null)        product.setStatus(request.getStatus());
        if (request.getCategoryId() != null)    product.setCategory(resolveCategory(request.getCategoryId()));

        // Replace images if provided
        if (request.getImageUrls() != null) {
            product.getImages().clear();
            List<ProductImage> images = buildImages(request.getImageUrls(), product);
            product.getImages().addAll(images);
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteProduct(UUID productId, UUID requesterId, String requesterRole) {
        Product product = findById(productId);

        if ("VENDOR".equals(requesterRole) && !product.getVendorId().equals(requesterId)) {
            throw new BadRequestException("You do not own this product");
        }

        // Soft delete
        product.setStatus(Product.Status.DELETED);
        productRepository.save(product);
        log.info("Product soft-deleted: id={}", productId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Product findActiveById(UUID id) {
        Product product = findById(id);
        if (product.getStatus() == Product.Status.DELETED) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        return product;
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + categoryId));
    }

    private List<ProductImage> buildImages(List<String> urls, Product product) {
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            images.add(ProductImage.builder()
                    .product(product)
                    .url(urls.get(i))
                    .displayOrder(i)
                    .isPrimary(i == 0) // first image is primary
                    .build());
        }
        return images;
    }

    private void publishProductCreatedEvent(Product product) {

    CompletableFuture.runAsync(() -> {
        try {
            String primaryImage = product.getImages().stream()
                    .filter(ProductImage::getIsPrimary)
                    .map(ProductImage::getUrl)
                    .findFirst()
                    .orElse(null);

            ProductCreatedEvent event = ProductCreatedEvent.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .vendorId(product.getVendorId())
                    .primaryImageUrl(primaryImage)
                    .build();

            kafkaTemplate.send("product-created", product.getId().toString(), event);

            log.info("ProductCreatedEvent published: productId={}", product.getId());

        } catch (Exception e) {
            log.error("Failed to publish ProductCreatedEvent for productId={}: {}",
                    product.getId(), e.getMessage());
        }
    });

}

    // ── Mapper ───────────────────────────────────────────────────────────────

    private ProductResponse toResponse(Product product) {
        List<String> imageUrls = product.getImages().stream()
                .sorted((a, b) -> a.getDisplayOrder() - b.getDisplayOrder())
                .map(ProductImage::getUrl)
                .collect(Collectors.toList());

        String primaryImageUrl = product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getUrl)
                .findFirst().orElse(null);

        return ProductResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .vendorId(product.getVendorId())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrls(imageUrls)
                .primaryImageUrl(primaryImageUrl)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}