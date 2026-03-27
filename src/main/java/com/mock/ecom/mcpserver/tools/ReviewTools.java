package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.Review;
import com.mock.ecom.mcpserver.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewTools {

    private final ReviewService reviewService;
    private final ToolResponseHelper helper;

    @Tool(description = "Submit a product review and star rating. Provide productId, rating (1-5 stars), review title, detailed description, and sessionId. Verified purchase badge is automatically assigned for customers who bought the product. Submitting again updates the existing review. Updates overall product rating.")
    public String submitProductReview(String productId, Integer rating, String title, String description, String sessionId) {
        try {
            log.info("[Tool] submitProductReview productId={} rating={} session={}", productId, rating, sessionId);
            if (rating == null) return helper.error("Rating is required (1-5)");
            Review review = reviewService.submitProductReview(productId, rating, title, description, sessionId);
            return helper.toJson(reviewToMap(review));
        } catch (Exception e) {
            log.error("[Tool] submitProductReview error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Submit a delivery experience review and star rating for a completed shipment. Rate the delivery speed, packaging quality, and delivery agent behavior. Provide shipmentId, rating (1-5), title, description, and sessionId. Helps improve delivery quality.")
    public String submitShipmentReview(String shipmentId, Integer rating, String title, String description, String sessionId) {
        try {
            log.info("[Tool] submitShipmentReview shipmentId={} rating={} session={}", shipmentId, rating, sessionId);
            if (rating == null) return helper.error("Rating is required (1-5)");
            Review review = reviewService.submitShipmentReview(shipmentId, rating, title, description, sessionId);
            return helper.toJson(reviewToMap(review));
        } catch (Exception e) {
            log.error("[Tool] submitShipmentReview error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    private Map<String, Object> reviewToMap(Review r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reviewId", r.getId() != null ? r.getId().toString() : null);
        m.put("rating", r.getRating());
        m.put("title", r.getTitle());
        m.put("description", r.getDescription());
        m.put("reviewType", r.getReviewType() != null ? r.getReviewType().name() : null);
        m.put("verifiedPurchase", r.isVerifiedPurchase());
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        m.put("message", "Review submitted successfully. Thank you for your feedback!");
        return m;
    }
}
