package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.ProductAttribute;
import com.mock.ecom.mcpserver.entity.Review;
import com.mock.ecom.mcpserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductDetailTools {

    private final ProductService productService;
    private final ToolResponseHelper helper;

    @Tool(description = "Get complete product details including title, full description, price, MRP, discount percentage, all product attributes (size, color, material, specifications), availability status, and all images for a given product ID. Use after searchProducts to show full product info.")
    public String getProductDetails(String productId) {
        try {
            log.info("[Tool] getProductDetails productId={}", productId);
            Product p = productService.getProductById(UUID.fromString(productId));
            List<ProductAttribute> attrs = productService.getProductAttributes(p);
            Map<String, Object> m = helper.productToMap(p);
            m.put("additionalImages", p.getAdditionalImages());
            m.put("size", p.getSize());
            m.put("color", p.getColor());
            m.put("material", p.getMaterial());
            m.put("weight", p.getWeight());
            m.put("attributes", attrs.stream().map(a -> Map.of("name", a.getName(), "value", a.getValue())).toList());
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] getProductDetails error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get overall product rating summary and paginated list of customer reviews for a product. Returns average rating, rating distribution (5-star, 4-star count etc.), and individual reviews with title, description, rating, verified purchase badge, and helpful count.")
    public String getProductReviews(String productId, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] getProductReviews productId={}", productId);
            int p = page != null ? page : 0;
            int s = pageSize != null ? pageSize : 10;
            Product product = productService.getProductById(UUID.fromString(productId));
            Page<Review> reviews = productService.getProductReviews(UUID.fromString(productId), p, s);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("productId", productId);
            summary.put("averageRating", product.getAverageRating());
            summary.put("totalReviews", product.getReviewCount());
            summary.put("page", p);
            summary.put("totalPages", reviews.getTotalPages());
            summary.put("reviews", reviews.getContent().stream().map(r -> {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("id", r.getId() != null ? r.getId().toString() : null);
                rm.put("rating", r.getRating());
                rm.put("title", r.getTitle());
                rm.put("description", r.getDescription());
                rm.put("verifiedPurchase", r.isVerifiedPurchase());
                rm.put("helpfulCount", r.getHelpfulCount());
                rm.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                return rm;
            }).toList());
            return helper.toJson(summary);
        } catch (Exception e) {
            log.error("[Tool] getProductReviews error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
