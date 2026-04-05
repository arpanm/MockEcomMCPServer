package com.mock.ecom.mcpserver.api;

import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.Review;
import com.mock.ecom.mcpserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ProductApiController {

    private final ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Product> results = productService.filterProducts(q, category, subCategory, brand, page, Math.min(size, 50));

        var products = results.getContent().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId().toString());
            map.put("title", p.getTitle());
            map.put("category", p.getCategory());
            map.put("subCategory", p.getSubCategory());
            map.put("brand", p.getBrand());
            map.put("model", p.getModel());
            map.put("price", p.getPrice());
            map.put("mrp", p.getMrp());
            map.put("averageRating", p.getAverageRating());
            map.put("reviewCount", p.getReviewCount());
            map.put("stockQuantity", p.getStockQuantity());
            map.put("imageUrl", p.getImageUrl());
            String desc = p.getDescription();
            map.put("description", desc != null && desc.length() > 200 ? desc.substring(0, 200) : desc);
            return map;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", results.getNumber());
        result.put("size", results.getSize());
        result.put("totalElements", results.getTotalElements());
        result.put("totalPages", results.getTotalPages());
        result.put("products", products);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/filters")
    public ResponseEntity<Map<String, Object>> getProductFilters(
            @RequestParam(defaultValue = "") String q) {
        Map<String, Object> filters = productService.getFilters(q);
        return ResponseEntity.ok(filters);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable String productId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid product ID format"));
        }

        Product product;
        try {
            product = productService.getProductById(uuid);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        }

        var attributes = productService.getProductAttributes(product).stream().map(attr -> {
            Map<String, Object> attrMap = new LinkedHashMap<>();
            attrMap.put("name", attr.getName());
            attrMap.put("value", attr.getValue());
            return attrMap;
        }).toList();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId().toString());
        map.put("title", product.getTitle());
        map.put("category", product.getCategory());
        map.put("subCategory", product.getSubCategory());
        map.put("brand", product.getBrand());
        map.put("model", product.getModel());
        map.put("price", product.getPrice());
        map.put("mrp", product.getMrp());
        map.put("averageRating", product.getAverageRating());
        map.put("reviewCount", product.getReviewCount());
        map.put("stockQuantity", product.getStockQuantity());
        map.put("imageUrl", product.getImageUrl());
        map.put("description", product.getDescription());
        map.put("size", product.getSize());
        map.put("color", product.getColor());
        map.put("material", product.getMaterial());
        map.put("weight", product.getWeight());
        map.put("additionalImages", product.getAdditionalImages());
        map.put("attributes", attributes);

        return ResponseEntity.ok(map);
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<Map<String, Object>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID uuid;
        try {
            uuid = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid product ID format"));
        }

        Page<Review> results;
        try {
            results = productService.getProductReviews(uuid, page, size);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        }

        var reviews = results.getContent().stream().map(r -> {
            Map<String, Object> rMap = new LinkedHashMap<>();
            rMap.put("id", r.getId() != null ? r.getId().toString() : null);
            rMap.put("rating", r.getRating());
            rMap.put("title", r.getTitle());
            rMap.put("description", r.getDescription());
            rMap.put("isVerifiedPurchase", r.isVerifiedPurchase());
            rMap.put("helpfulCount", r.getHelpfulCount());
            rMap.put("createdAt", r.getCreatedAt());
            return rMap;
        }).toList();

        double avgRating = reviews.stream()
                .mapToInt(r -> (Integer) r.get("rating"))
                .average()
                .orElse(0.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", results.getNumber());
        result.put("totalElements", results.getTotalElements());
        result.put("totalPages", results.getTotalPages());
        result.put("reviews", reviews);
        result.put("averageRating", avgRating);
        return ResponseEntity.ok(result);
    }
}
