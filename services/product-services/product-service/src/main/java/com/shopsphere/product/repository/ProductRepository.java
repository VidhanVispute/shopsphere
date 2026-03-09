package com.shopsphere.product.repository;

import com.shopsphere.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    // Vendor's own products (excluding deleted)
    Page<Product> findByVendorIdAndStatusNot(UUID vendorId,
            Product.Status status, Pageable pageable);

    // Public listing — only ACTIVE
    Page<Product> findByStatus(Product.Status status, Pageable pageable);

    // Check ownership
    Optional<Product> findByIdAndVendorId(UUID id, UUID vendorId);
}