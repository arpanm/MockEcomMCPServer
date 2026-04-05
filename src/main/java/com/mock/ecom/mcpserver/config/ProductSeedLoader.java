package com.mock.ecom.mcpserver.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.ProductAttribute;
import com.mock.ecom.mcpserver.repository.ProductAttributeRepository;
import com.mock.ecom.mcpserver.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loads pre-generated product seed data from {@code classpath:db/seed/products-seed.json}
 * on application startup if the product table is empty.
 *
 * <p>Ordering:
 * <ol>
 *   <li>Order 1 – {@code CityDataInitializer} (seeds 50 cities)</li>
 *   <li>Order 2 – {@code SeedDataLoader} (loads scraped restaurant + menu data)</li>
 *   <li>Order 3 – This loader (loads product seed data)</li>
 *   <li>Order 4 – {@code SampleRestaurantDataSeeder} (fallback demo data if still empty)</li>
 *   <li>Order 5 – {@code ScraperAutoStartRunner} (optionally starts live scraper)</li>
 * </ol>
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class ProductSeedLoader implements ApplicationRunner {

    private static final String SEED_FILE = "db/seed/products-seed.json";

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathResource resource = new ClassPathResource(SEED_FILE);
        if (!resource.exists()) {
            log.info("No product seed file found at classpath:{}. Skipping product seed data load.", SEED_FILE);
            return;
        }

        if (productRepository.count() > 0) {
            log.info("Products already present in DB ({}). Skipping product seed data load.", productRepository.count());
            return;
        }

        log.info("Loading product seed data from classpath:{}", SEED_FILE);
        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode productsArray = root.path("products");

            if (!productsArray.isArray()) {
                log.error("Invalid product seed file format: expected 'products' array");
                return;
            }

            int loaded = 0;
            int skipped = 0;

            for (JsonNode node : productsArray) {
                try {
                    String title = node.path("title").asText("Unknown Product");
                    String category = node.path("category").asText("ELECTRONICS");

                    // Generate unique searchKey
                    String searchKeyBase = category.toLowerCase() + "_"
                            + title.toLowerCase().replaceAll("[^a-z0-9]", "_");
                    String searchKey = searchKeyBase + "_"
                            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

                    double price = node.path("price").asDouble(999.0);
                    double mrp = node.path("mrp").asDouble(price * 1.2);
                    if (mrp < price) mrp = price;

                    Product product = Product.builder()
                            .title(title)
                            .description(nullIfBlank(node.path("description").asText(null)))
                            .category(category)
                            .subCategory(nullIfBlank(node.path("subCategory").asText(null)))
                            .brand(nullIfBlank(node.path("brand").asText(null)))
                            .model(nullIfBlank(node.path("model").asText(null)))
                            .imageUrl(nullIfBlank(node.path("imageUrl").asText(null)))
                            .price(BigDecimal.valueOf(price))
                            .mrp(BigDecimal.valueOf(mrp))
                            .currency(node.path("currency").asText("INR"))
                            .size(nullIfBlank(node.path("size").asText(null)))
                            .color(nullIfBlank(node.path("color").asText(null)))
                            .material(nullIfBlank(node.path("material").asText(null)))
                            .weight(nullIfBlank(node.path("weight").asText(null)))
                            .stockQuantity(node.path("stockQuantity").asInt(100))
                            .searchKey(searchKey)
                            .averageRating(node.path("averageRating").asDouble(4.0))
                            .reviewCount(node.path("reviewCount").asInt(100))
                            .build();

                    Product saved = productRepository.save(product);

                    // Save attributes
                    JsonNode attrsArray = node.path("attributes");
                    if (attrsArray.isArray()) {
                        List<ProductAttribute> attrs = new ArrayList<>();
                        for (JsonNode attrNode : attrsArray) {
                            String key = attrNode.path("attributeKey").asText(null);
                            String value = attrNode.path("attributeValue").asText(null);
                            if (key != null && !key.isBlank()) {
                                attrs.add(ProductAttribute.builder()
                                        .product(saved)
                                        .name(key)
                                        .value(value)
                                        .build());
                            }
                        }
                        if (!attrs.isEmpty()) {
                            productAttributeRepository.saveAll(attrs);
                        }
                    }

                    loaded++;
                } catch (Exception e) {
                    log.warn("Failed to load product from seed: {}", e.getMessage());
                    skipped++;
                }
            }

            log.info("Product seed data loaded: {} products imported, {} skipped.", loaded, skipped);
        } catch (Exception e) {
            log.error("Failed to load product seed data: {}", e.getMessage(), e);
        }
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank() || s.equalsIgnoreCase("null")) ? null : s;
    }
}
