package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.ProductAttribute;
import com.mock.ecom.mcpserver.entity.Review;
import com.mock.ecom.mcpserver.repository.ProductAttributeRepository;
import com.mock.ecom.mcpserver.repository.ProductRepository;
import com.mock.ecom.mcpserver.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ReviewRepository reviewRepository;
    private final MockDataGeneratorService mockDataGenerator;

    private static final int DEFAULT_GENERATE_COUNT = 8;

    public Page<Product> searchProducts(String query, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> existing = productRepository.searchByQuery(query, pageable);
        if (existing.getTotalElements() == 0) {
            List<Product> generated = mockDataGenerator.generateProductList(query, DEFAULT_GENERATE_COUNT);
            int start = Math.min(page * pageSize, generated.size());
            int end   = Math.min(start + pageSize, generated.size());
            return new PageImpl<>(generated.subList(start, end), pageable, generated.size());
        }
        return existing;
    }

    public Page<Product> filterProducts(String query, String category, String subCategory, String brand, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        if (query != null && !query.isBlank()) {
            Page<Product> base = productRepository.searchByQuery(query, PageRequest.of(0, 100));
            if (base.getTotalElements() == 0) {
                mockDataGenerator.generateProductList(query, DEFAULT_GENERATE_COUNT);
            }
        }
        return productRepository.filterProducts(
            category != null && !category.isBlank() ? category : null,
            subCategory != null && !subCategory.isBlank() ? subCategory : null,
            brand != null && !brand.isBlank() ? brand : null,
            pageable);
    }

    public Map<String, Object> getFilters(String query) {
        if (query != null && !query.isBlank()) {
            Page<Product> existing = productRepository.searchByQuery(query, PageRequest.of(0, 1));
            if (existing.getTotalElements() == 0) {
                mockDataGenerator.generateProductList(query, DEFAULT_GENERATE_COUNT);
            }
        }
        List<String> categories   = productRepository.findDistinctCategoriesByQuery(query != null ? query : "");
        List<String> subCategories= productRepository.findDistinctSubCategoriesByQuery(query != null ? query : "");
        List<String> brands       = productRepository.findDistinctBrandsByQuery(query != null ? query : "");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("categories",    categories.stream().map(c -> Map.of("name", c)).toList());
        result.put("subCategories", subCategories.stream().map(s -> Map.of("name", s)).toList());
        result.put("brands",        brands.stream().map(b -> Map.of("name", b)).toList());
        return result;
    }

    public Product getProductById(UUID productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    public List<ProductAttribute> getProductAttributes(Product product) {
        return productAttributeRepository.findByProduct(product);
    }

    public Page<Review> getProductReviews(UUID productId, int page, int pageSize) {
        Product product = getProductById(productId);
        Page<Review> reviews = reviewRepository.findByProductOrderByCreatedAtDesc(product, PageRequest.of(page, pageSize));
        if (reviews.getTotalElements() == 0) {
            return new PageImpl<>(generateMockReviews(product), PageRequest.of(page, pageSize), 5);
        }
        return reviews;
    }

    private List<Review> generateMockReviews(Product product) {
        long seed = Math.abs(product.getId().hashCode());
        List<Review> reviews = new ArrayList<>();
        String[] titles   = {"Great product!","Highly recommended","Value for money","Good quality","Satisfied with purchase"};
        String[] descs    = {"Exactly as described. Very happy with this purchase.","Quality exceeded my expectations. Will buy again.","Good product for the price. Delivery was fast.","Works perfectly. Easy to use.","Decent product. Does the job well."};
        int[] ratings = {5, 4, 5, 4, 3};
        for (int i = 0; i < 5; i++) {
            reviews.add(Review.builder()
                .product(product).rating(ratings[i])
                .title(titles[(int)((seed + i) % titles.length)])
                .description(descs[(int)((seed + i) % descs.length)])
                .helpfulCount((int)((seed + i * 7) % 50))
                .isVerifiedPurchase(i % 2 == 0)
                .reviewType(Review.ReviewType.PRODUCT)
                .build());
        }
        return reviews;
    }
}
