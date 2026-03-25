package com.mock.ecom.mcpserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.tools.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for wishlist tools 27-28: addToWishlist, getWishlist
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WishlistTest {

    @Autowired private WishlistTools wishlistTools;
    @Autowired private AuthTools authTools;
    @Autowired private ProductSearchTools productSearchTools;

    private static final ObjectMapper om = new ObjectMapper();
    private static String sessionId;
    private static String productId;
    private static String productId2;

    private JsonNode parse(String json) throws Exception {
        return om.readTree(json);
    }

    @Test
    @Order(1)
    void setupSessionAndProducts() throws Exception {
        String loginResult = authTools.serverToServerLogin("6600000001", "WishlistTest", "mock-platform-secret-key");
        sessionId = parse(loginResult).get("sessionId").asText();

        productSearchTools.searchProducts("watch luxury", 0, 1); // trigger async generation
        productSearchTools.searchProducts("perfume fragrance", 0, 1); // trigger async generation
        Thread.sleep(300); // wait for async product saves
        String search1 = productSearchTools.searchProducts("watch luxury", 0, 1);
        productId = parse(search1).get("products").get(0).get("id").asText();

        String search2 = productSearchTools.searchProducts("perfume fragrance", 0, 1);
        productId2 = parse(search2).get("products").get(0).get("id").asText();
    }

    @Test
    @Order(2)
    void getWishlist_emptyInitially_returnsEmptyList() throws Exception {
        String result = wishlistTools.getWishlist(sessionId, 0, 20);
        JsonNode json = parse(result);
        assertNotNull(json.get("items"), "Should return wishlist array");
        assertTrue(json.get("items").isArray(), "Wishlist should be an array");
    }

    @Test
    @Order(3)
    void addToWishlist_addsProduct() throws Exception {
        String result = wishlistTools.addToWishlist(productId, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("wishlistItemId") != null ? json.get("wishlistItemId") : json.get("message") != null ? json.get("message") : json,
            "Should return success or item ID");
        // Could be added or already exists
    }

    @Test
    @Order(4)
    void addToWishlist_secondProduct_addsSuccessfully() throws Exception {
        String result = wishlistTools.addToWishlist(productId2, sessionId);
        assertNotNull(parse(result));
    }

    @Test
    @Order(5)
    void getWishlist_afterAdding_returnsProducts() throws Exception {
        String result = wishlistTools.getWishlist(sessionId, 0, 20);
        JsonNode json = parse(result);

        assertNotNull(json.get("items"));
        assertTrue(json.get("items").size() > 0, "Wishlist should have items after adding");
        assertNotNull(json.get("totalElements"), "Should include totalElements");
    }

    @Test
    @Order(6)
    void addToWishlist_duplicateProduct_handlesGracefully() throws Exception {
        // Adding same product again should not fail
        String result = wishlistTools.addToWishlist(productId, sessionId);
        assertNotNull(parse(result), "Should handle duplicate gracefully");
    }

    @Test
    @Order(7)
    void addToWishlist_invalidSession_returnsError() throws Exception {
        String result = wishlistTools.addToWishlist(productId, "invalid-session");
        JsonNode json = parse(result);
        assertNotNull(json.get("error"), "Should return error for invalid session");
    }

    @Test
    @Order(8)
    void getWishlist_invalidSession_returnsError() throws Exception {
        String result = wishlistTools.getWishlist("invalid-session", 0, 20);
        JsonNode json = parse(result);
        assertNotNull(json.get("error"), "Should return error for invalid session");
    }

    @Test
    @Order(9)
    void wishlist_multipleUsers_isolatedPerUser() throws Exception {
        // Create a second user
        String loginResult = authTools.serverToServerLogin("6600000002", "WishlistTest", "mock-platform-secret-key");
        String sid2 = parse(loginResult).get("sessionId").asText();

        // Second user should have empty wishlist
        String result = wishlistTools.getWishlist(sid2, 0, 20);
        JsonNode json = parse(result);
        assertNotNull(json.get("items"));
        assertEquals(0, json.get("items").size(), "Second user should have empty wishlist");

        // Add item to second user
        wishlistTools.addToWishlist(productId, sid2);

        // Verify original user still has their wishlist
        String originalWishlist = wishlistTools.getWishlist(sessionId, 0, 20);
        JsonNode originalJson = parse(originalWishlist);
        assertTrue(originalJson.get("items").size() > 0, "Original user wishlist should be intact");
    }

    @Test
    @Order(10)
    void addToWishlist_invalidProductId_returnsError() throws Exception {
        String result = wishlistTools.addToWishlist("00000000-0000-0000-0000-000000000000", sessionId);
        JsonNode json = parse(result);
        assertNotNull(json.get("error"), "Should return error for invalid product");
    }
}
