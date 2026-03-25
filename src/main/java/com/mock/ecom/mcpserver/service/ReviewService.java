package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuthService authService;
    private final ProductService productService;

    @Transactional
    public Review submitProductReview(String productId, int rating, String title, String description, String sessionId) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be between 1 and 5");
        Customer customer = authService.getCustomerFromSession(sessionId);
        Product product = productService.getProductById(UUID.fromString(productId));
        Review existing = reviewRepository.findByProductAndCustomer(product, customer).orElse(null);
        if (existing != null) {
            existing.setRating(rating); existing.setTitle(title); existing.setDescription(description);
            return reviewRepository.save(existing);
        }
        Review review = reviewRepository.save(Review.builder()
            .product(product).customer(customer).rating(rating)
            .title(title).description(description)
            .isVerifiedPurchase(true)
            .reviewType(Review.ReviewType.PRODUCT).build());
        updateProductRating(product);
        return review;
    }

    @Transactional
    public Review submitShipmentReview(String shipmentId, int rating, String title, String description, String sessionId) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be between 1 and 5");
        Customer customer = authService.getCustomerFromSession(sessionId);
        Shipment shipment = shipmentRepository.findById(UUID.fromString(shipmentId))
            .filter(s -> s.getOrder().getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
        return reviewRepository.save(Review.builder()
            .shipment(shipment).customer(customer).rating(rating)
            .title(title).description(description)
            .isVerifiedPurchase(true)
            .reviewType(Review.ReviewType.DELIVERY).build());
    }

    private void updateProductRating(Product product) {
        Double avg = reviewRepository.findAverageRatingByProduct(product);
        long count = reviewRepository.countByProduct(product);
        if (avg != null) {
            product.setAverageRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue());
            product.setReviewCount((int) count);
            productRepository.save(product);
        }
    }
}
