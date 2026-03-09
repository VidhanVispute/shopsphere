package com.shopsphere.product.service;

import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.product.dto.request.CreateCategoryRequest;
import com.shopsphere.product.dto.response.CategoryResponse;
import com.shopsphere.product.entity.Category;
import com.shopsphere.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ── Public ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIsNull();
        return roots.stream()
                .map(this::toResponseWithChildren)
                .collect(Collectors.toList());
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category name already exists: " + request.getName());
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent category not found: " + request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parent(parent)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, name={}", saved.getId(), saved.getName());
        return toResponseWithChildren(saved);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private CategoryResponse toResponseWithChildren(Category category) {
        List<CategoryResponse> children = category.getChildren().stream()
                .map(this::toResponseWithChildren)
                .collect(Collectors.toList());

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(children)
                .build();
    }

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(List.of())
                .build();
    }
}