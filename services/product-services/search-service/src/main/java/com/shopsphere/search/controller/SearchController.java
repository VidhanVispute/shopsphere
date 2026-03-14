package com.shopsphere.search.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.search.document.ProductDocument;
import com.shopsphere.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDocument>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        List<ProductDocument> results = searchService.search(q, category, minPrice, maxPrice);
        return ResponseEntity.ok(ApiResponse.success("Search results", results));
    }
}