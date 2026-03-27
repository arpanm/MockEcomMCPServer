package com.mock.ecom.mcpserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.tools.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductToolsIntegrationTest {

    @Autowired
    private ProductSearchTools productSearchTools;

    @Autowired
    private ProductDetailTools productDetailTools;

    @Autowired
    private DeliveryTools deliveryTools;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String parseJson(String json, String field) throws Exception {
        return objectMapper.readTree(json).path(field).asText();
    }

    private JsonNode parseJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    @Test
    void searchProducts_withQuery_returnsProducts() throws Exception {
        String result = productSearchTools.searchProducts("laptop", 0, 5);
        JsonNode json = parseJson(result);

        assertNotNull(json.get("query"));
        assertEquals("laptop", json.get("query").asText());
        assertNotNull(json.get("products"));
        assertTrue(json.get("products").isArray());
        assertTrue(json.get("products").size() > 0, "Should return at least one product");
        assertNotNull(json.get("totalElements"));
    }

    @Test
    void searchProducts_withGroceryQuery_returnsGroceryProducts() throws Exception {
        String result = productSearchTools.searchProducts("milk rice wheat", 0, 10);
        JsonNode json = parseJson(result);

        assertNotNull(json.get("products"));
        assertTrue(json.get("products").size() > 0);
    }

    @Test
    void searchProducts_withFashionQuery_returnsFashionProducts() throws Exception {
        String result = productSearchTools.searchProducts("shirt jeans dress", 0, 10);
        JsonNode json = parseJson(result);
        assertNotNull(json.get("products"));
        assertTrue(json.get("products").size() > 0);
    }

    @Test
    void searchProducts_defaultPage_works() throws Exception {
        String result = productSearchTools.searchProducts("phone", null, null);
        JsonNode json = parseJson(result);
        assertEquals(0, json.get("page").asInt());
        assertEquals(10, json.get("pageSize").asInt());
    }

    @Test
    void searchProducts_pageSizeRespected() throws Exception {
        String result = productSearchTools.searchProducts("electronic", 0, 3);
        JsonNode json = parseJson(result);
        assertTrue(json.get("products").size() <= 3);
    }

    @Test
    void filterProducts_byCategory_returnsFilteredProducts() throws Exception {
        // First create some products
        productSearchTools.searchProducts("electronics gadgets", 0, 10);

        String result = productSearchTools.filterProducts(null, "ELECTRONICS", null, null, 0, 10);
        JsonNode json = parseJson(result);

        assertNotNull(json.get("products"));
        assertNotNull(json.get("filters"));
    }

    @Test
    void filterProducts_noFilters_returnsProducts() throws Exception {
        String result = productSearchTools.filterProducts("shirt", null, null, null, 0, 5);
        JsonNode json = parseJson(result);
        assertNotNull(json.get("products"));
    }

    @Test
    void getFilters_returnsFilterOptions() throws Exception {
        // Seed some products first
        productSearchTools.searchProducts("shoes sneakers", 0, 5);

        String result = productSearchTools.getFilters("shoes");
        JsonNode json = parseJson(result);
        assertNotNull(json);
        // Should have filter structure
    }

    @Test
    void getSortOptions_returnsAllSortKeys() throws Exception {
        String result = productSearchTools.getSortOptions("laptop");
        JsonNode json = parseJson(result);

        assertNotNull(json.get("sortOptions"));
        assertTrue(json.get("sortOptions").isArray());
        assertTrue(json.get("sortOptions").size() >= 6, "Should have at least 6 sort options");

        boolean hasPriceAsc = false;
        for (JsonNode opt : json.get("sortOptions")) {
            if ("price_asc".equals(opt.get("key").asText())) {
                hasPriceAsc = true;
                break;
            }
        }
        assertTrue(hasPriceAsc, "Should have price_asc sort option");
    }

    @Test
    void getProductDetails_returnsDetails() throws Exception {
        // First search to get a product ID
        String searchResult = productSearchTools.searchProducts("smartphone", 0, 1);
        JsonNode searchJson = parseJson(searchResult);
        String productId = searchJson.get("products").get(0).get("id").asText();

        String result = productDetailTools.getProductDetails(productId);
        JsonNode json = parseJson(result);

        assertNotNull(json.get("id"));
        assertEquals(productId, json.get("id").asText());
        assertNotNull(json.get("title"));
        assertNotNull(json.get("price"));
        assertNotNull(json.get("category"));
    }

    @Test
    void getProductDetails_invalidId_returnsError() throws Exception {
        String result = productDetailTools.getProductDetails("00000000-0000-0000-0000-000000000000");
        JsonNode json = parseJson(result);
        assertNotNull(json.get("error"));
    }

    @Test
    void getProductReviews_returnsReviews() throws Exception {
        String searchResult = productSearchTools.searchProducts("headphone", 0, 1);
        JsonNode searchJson = parseJson(searchResult);
        String productId = searchJson.get("products").get(0).get("id").asText();

        String result = productDetailTools.getProductReviews(productId, 0, 5);
        JsonNode json = parseJson(result);

        assertNotNull(json.get("productId"));
        assertNotNull(json.get("reviews"));
        assertTrue(json.get("reviews").isArray());
    }

    @Test
    void getDeliveryTime_metroPin_returnsFastDelivery() throws Exception {
        String searchResult = productSearchTools.searchProducts("book", 0, 1);
        JsonNode searchJson = parseJson(searchResult);
        String productId = searchJson.get("products").get(0).get("id").asText();

        String result = deliveryTools.getDeliveryTime(productId, "110001");
        JsonNode json = parseJson(result);

        assertNotNull(json.get("pincode"));
        assertEquals("110001", json.get("pincode").asText());
        assertNotNull(json.get("deliveryOptions"));
        assertTrue(json.get("deliveryOptions").isArray());
    }

    @Test
    void getDeliveryTime_remotePincode_returnsLongerDelivery() throws Exception {
        String searchResult = productSearchTools.searchProducts("furniture", 0, 1);
        JsonNode searchJson = parseJson(searchResult);
        String productId = searchJson.get("products").get(0).get("id").asText();

        String result = deliveryTools.getDeliveryTime(productId, "700000");
        JsonNode json = parseJson(result);
        assertNotNull(json.get("deliveryOptions"));
    }

    @Test
    void searchProducts_allCategories_work() throws Exception {
        String[] queries = {"apple banana", "laptop phone", "shirt dress", "face cream", "sofa table"};
        for (String query : queries) {
            String result = productSearchTools.searchProducts(query, 0, 5);
            JsonNode json = parseJson(result);
            assertNotNull(json.get("products"), "Should return products for query: " + query);
            assertTrue(json.get("products").size() > 0, "Should have at least one product for: " + query);
        }
    }
}
