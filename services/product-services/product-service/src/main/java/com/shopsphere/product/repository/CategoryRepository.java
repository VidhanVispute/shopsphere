package com.shopsphere.product.repository;

import com.shopsphere.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // All root categories (parent is null)
    List<Category> findByParentIsNull();

    // All children of a given parent
    List<Category> findByParentId(UUID parentId);

    boolean existsByName(String name);
}