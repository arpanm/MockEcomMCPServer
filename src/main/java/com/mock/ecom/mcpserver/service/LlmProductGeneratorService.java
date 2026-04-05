package com.mock.ecom.mcpserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.ProductAttribute;
import com.mock.ecom.mcpserver.repository.ProductAttributeRepository;
import com.mock.ecom.mcpserver.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class LlmProductGeneratorService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.llm.api-key:}")
    private String apiKey;

    @Value("${app.llm.model:claude-haiku-4-5-20251001}")
    private String model;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final Random random = new Random();

    public LlmProductGeneratorService(ProductRepository productRepository,
                                       ProductAttributeRepository productAttributeRepository,
                                       ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<Product> generateProducts(String category, String query, int count) {
        if (!isAvailable()) {
            log.warn("LLM API key not configured, skipping LLM product generation");
            return Collections.emptyList();
        }

        try {
            String prompt = buildPrompt(category, query, count);
            String responseText = callAnthropicApi(prompt);
            return parseAndSaveProducts(responseText);
        } catch (Exception e) {
            log.error("LLM product generation failed for query='{}', category='{}': {}", query, category, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String buildPrompt(String category, String query, int count) {
        return String.format("""
                Generate %d realistic Indian market ecommerce products for the search query "%s" in the "%s" category.

                Return ONLY a valid JSON array (no markdown, no explanation) with exactly %d product objects.
                Each product object must have these fields:
                - title: string (product title with brand and model)
                - description: string (detailed product description, 2-3 sentences)
                - category: string (one of: ELECTRONICS, FASHION, GROCERY, BEAUTY, HOME)
                - subCategory: string (subcategory name)
                - brand: string (real Indian or international brand sold in India)
                - model: string (model number/name)
                - imageUrl: string (use format: https://picsum.photos/seed/PRODUCTNAME/400/400)
                - price: number (selling price in INR, realistic for Indian market)
                - mrp: number (MRP in INR, must be >= price)
                - currency: "INR"
                - size: string (size if applicable, else null)
                - color: string (color if applicable, else null)
                - material: string (material if applicable, else null)
                - weight: string (weight if applicable, else null)
                - stockQuantity: integer (between 10 and 500)
                - attributes: object (key-value map of additional product attributes like warranty, processor, RAM for electronics; fabric, fit for fashion; etc.)

                Use REAL Indian brands like Samsung, OnePlus, Boat, Lakme, Himalaya, Tata, Fortune, Amul, Bata, Fabindia, Prestige, Philips, Sony, Xiaomi, etc.
                Use realistic INR pricing (price should be less than mrp, meaningful discounts).
                Make products diverse and specific to the query.

                Return only the JSON array, starting with [ and ending with ].
                """, count, query, category, count);
    }

    private String callAnthropicApi(String prompt) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 4096);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic API returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("content");
        if (content.isEmpty()) {
            throw new RuntimeException("Empty content in Anthropic API response");
        }
        return content.get(0).path("text").asText();
    }

    private List<Product> parseAndSaveProducts(String jsonText) throws Exception {
        // Extract JSON array from response (handle potential markdown code blocks)
        String cleanJson = jsonText.trim();
        int startIdx = cleanJson.indexOf('[');
        int endIdx = cleanJson.lastIndexOf(']');
        if (startIdx >= 0 && endIdx > startIdx) {
            cleanJson = cleanJson.substring(startIdx, endIdx + 1);
        }

        JsonNode productsArray = objectMapper.readTree(cleanJson);
        if (!productsArray.isArray()) {
            throw new RuntimeException("Expected JSON array from LLM, got: " + productsArray.getNodeType());
        }

        List<Product> savedProducts = new ArrayList<>();

        for (JsonNode node : productsArray) {
            try {
                Product product = buildProductFromNode(node);
                Product saved = productRepository.save(product);

                // Save attributes
                JsonNode attrsNode = node.path("attributes");
                if (attrsNode.isObject()) {
                    List<ProductAttribute> attrs = new ArrayList<>();
                    attrsNode.fields().forEachRemaining(entry -> {
                        attrs.add(ProductAttribute.builder()
                                .product(saved)
                                .name(entry.getKey())
                                .value(entry.getValue().asText())
                                .build());
                    });
                    productAttributeRepository.saveAll(attrs);
                }

                savedProducts.add(saved);
                log.debug("LLM: saved product '{}'", saved.getTitle());
            } catch (Exception e) {
                log.warn("Failed to save LLM-generated product: {}", e.getMessage());
            }
        }

        log.info("LLM: generated and saved {} products", savedProducts.size());
        return savedProducts;
    }

    private Product buildProductFromNode(JsonNode node) {
        String title = node.path("title").asText("Unknown Product");
        String category = node.path("category").asText("ELECTRONICS");

        // Generate unique searchKey
        String searchKeyBase = category.toLowerCase() + "_" + title.toLowerCase().replaceAll("[^a-z0-9]", "_");
        String searchKey = searchKeyBase + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        double price = node.path("price").asDouble(999.0);
        double mrp = node.path("mrp").asDouble(price * 1.2);
        if (mrp < price) mrp = price;

        // Random rating between 3.5 and 4.8
        double rating = Math.round((3.5 + random.nextDouble() * 1.3) * 10.0) / 10.0;
        // Random review count between 50 and 5000
        int reviewCount = 50 + random.nextInt(4951);

        String imageUrl = node.path("imageUrl").asText(null);
        if (imageUrl == null || imageUrl.isBlank()) {
            String urlSlug = title.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
            imageUrl = "https://picsum.photos/seed/" + urlSlug + "/400/400";
        }

        return Product.builder()
                .title(title)
                .description(node.path("description").asText(null))
                .category(category)
                .subCategory(node.path("subCategory").asText(null))
                .brand(node.path("brand").asText(null))
                .model(node.path("model").asText(null))
                .imageUrl(imageUrl)
                .price(BigDecimal.valueOf(price))
                .mrp(BigDecimal.valueOf(mrp))
                .currency(node.path("currency").asText("INR"))
                .size(nullIfBlank(node.path("size").asText(null)))
                .color(nullIfBlank(node.path("color").asText(null)))
                .material(nullIfBlank(node.path("material").asText(null)))
                .weight(nullIfBlank(node.path("weight").asText(null)))
                .stockQuantity(node.path("stockQuantity").asInt(100))
                .searchKey(searchKey)
                .averageRating(rating)
                .reviewCount(reviewCount)
                .build();
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank() || s.equalsIgnoreCase("null")) ? null : s;
    }
}
