package com.shopsphere.search.service;

import com.shopsphere.common.dto.ProductEvent;
import com.shopsphere.search.document.ProductDocument;
import com.shopsphere.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // ── Indexing ──────────────────────────────────────────────────────────────

    public void indexProduct(ProductEvent event) {
        ProductDocument doc = ProductDocument.builder()
                .id(event.getProductId().toString())
                .name(event.getName())
                .description(event.getDescription())
                .price(event.getPrice())
                .categoryId(event.getCategoryId() != null ? event.getCategoryId().toString() : null)
                .categoryName(event.getCategoryName())
                .vendorId(event.getVendorId() != null ? event.getVendorId().toString() : null)
                .status(event.getStatus())
                .primaryImageUrl(event.getPrimaryImageUrl())
                .build();

        searchRepository.save(doc);
        log.info("Indexed product: {}", event.getProductId());
    }

    public void deleteFromIndex(ProductEvent event) {
        searchRepository.deleteById(event.getProductId().toString());
        log.info("Removed product from index: {}", event.getProductId());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<ProductDocument> search(String q, String category,
                                         BigDecimal minPrice, BigDecimal maxPrice) {
        Criteria criteria = new Criteria();

        // Full-text search on name + description
        if (q != null && !q.isBlank()) {
            criteria = criteria.and(
                new Criteria("name").matches(q)
                    .or(new Criteria("description").matches(q))
            );
        }

        // Filter by category
        if (category != null && !category.isBlank()) {
            criteria = criteria.and(new Criteria("categoryName").is(category));
        }

        // Price range
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice));
        }

        // Only ACTIVE products
        criteria = criteria.and(new Criteria("status").is("ACTIVE"));

        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<ProductDocument> hits =
                elasticsearchOperations.search(query, ProductDocument.class);

        return hits.stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());
    }
}