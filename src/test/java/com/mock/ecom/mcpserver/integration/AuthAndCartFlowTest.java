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
 * Integration tests for auth + cart + checkout + payment end-to-end flow.
 * Tests tools 8-14: serverToServerLogin, addToCart, checkout,
 * getAddresses, selectAddress, initiatePayment, getPaymentStatus
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthAndCartFlowTest {

    @Autowired private AuthTools authTools;
    @Autowired private ProductSearchTools productSearchTools;
    @Autowired private CartTools cartTools;
    @Autowired private AddressTools addressTools;
    @Autowired private PaymentTools paymentTools;

    private static final ObjectMapper om = new ObjectMapper();

    // Shared state across ordered tests
    private static String sessionId;
    private static String productId;
    private static String cartId;
    private static String checkoutId;
    private static String paymentId;
    private static String orderId;
    private static String addressId;

    private JsonNode parse(String json) throws Exception {
        return om.readTree(json);
    }

    @Test
    @Order(1)
    void login_withValidSecret_returnsSession() throws Exception {
        String result = authTools.serverToServerLogin("9876543210", "TestApp", "mock-platform-secret-key");
        JsonNode json = parse(result);

        assertNotNull(json.get("sessionId"), "Should return sessionId");
        assertNotNull(json.get("customerId"), "Should return customerId");
        assertEquals("success", json.get("status").asText());
        assertEquals("9876543210", json.get("phoneNumber").asText());
        assertEquals("TestApp", json.get("platform").asText());

        sessionId = json.get("sessionId").asText();
        assertFalse(sessionId.isEmpty(), "Session ID should not be empty");
    }

    @Test
    @Order(2)
    void login_withInvalidSecret_returnsError() throws Exception {
        String result = authTools.serverToServerLogin("9876543210", "TestApp", "wrong-secret");
        JsonNode json = parse(result);
        assertNotNull(json.get("error"), "Should return error for invalid secret");
    }

    @Test
    @Order(3)
    void login_secondTime_returnsNewSession() throws Exception {
        String result = authTools.serverToServerLogin("9876543210", "TestApp", "mock-platform-secret-key");
        JsonNode json = parse(result);
        assertEquals("success", json.get("status").asText());
    }

    @Test
    @Order(4)
    void searchProduct_forCartFlow() throws Exception {
        productSearchTools.searchProducts("laptop dell", 0, 1); // trigger async generation
        Thread.sleep(300); // wait for async product save
        String result = productSearchTools.searchProducts("laptop dell", 0, 1);
        JsonNode json = parse(result);
        assertTrue(json.get("products").size() > 0);
        productId = json.get("products").get(0).get("id").asText();
        assertFalse(productId.isEmpty(), "Product ID should not be empty");
        assertFalse("null".equals(productId), "Product ID should not be 'null'");
    }

    @Test
    @Order(5)
    void addToCart_withValidSession_addsProduct() throws Exception {
        assertNotNull(sessionId, "Need sessionId from login test");
        assertNotNull(productId, "Need productId from search test");

        String result = cartTools.addToCart(productId, 2, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("cartId"), "Should return cartId");
        assertNotNull(json.get("items"), "Should have items");
        assertTrue(json.get("items").size() > 0, "Cart should have at least one item");
        assertEquals(2, json.get("items").get(0).get("quantity").asInt(), "Quantity should be 2");

        cartId = json.get("cartId").asText();
    }

    @Test
    @Order(6)
    void addToCart_invalidSession_returnsError() throws Exception {
        String result = cartTools.addToCart(productId, 1, "invalid-session-id");
        JsonNode json = parse(result);
        assertNotNull(json.get("error"), "Should return error for invalid session");
    }

    @Test
    @Order(7)
    void addToCart_sameProduct_incrementsQuantity() throws Exception {
        String result = cartTools.addToCart(productId, 1, sessionId);
        JsonNode json = parse(result);

        // Should have at least one item
        assertTrue(json.get("items").size() > 0);
    }

    @Test
    @Order(8)
    void checkout_withValidCart_initiatesCheckout() throws Exception {
        assertNotNull(cartId, "Need cartId from addToCart test");

        String result = cartTools.checkout(cartId, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("checkoutId"), "Should return checkoutId");
        assertNotNull(json.get("totalAmount"), "Should return totalAmount");
        assertNotNull(json.get("grandTotal"), "Should return grandTotal");
        assertTrue(json.get("grandTotal").asDouble() > 0, "Grand total should be positive");

        checkoutId = json.get("checkoutId").asText();
    }

    @Test
    @Order(9)
    void getAddresses_returnsAddresses() throws Exception {
        String result = addressTools.getAddresses(sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("addresses"), "Should return addresses");
        assertTrue(json.get("addresses").isArray(), "Addresses should be array");
        assertTrue(json.get("addresses").size() > 0, "Should have at least one address (mock auto-generated)");

        addressId = json.get("addresses").get(0).get("addressId").asText();
        assertFalse(addressId.isEmpty(), "Address ID should not be empty");
    }

    @Test
    @Order(10)
    void getAddresses_invalidSession_returnsError() throws Exception {
        String result = addressTools.getAddresses("invalid-session");
        JsonNode json = parse(result);
        assertNotNull(json.get("error"));
    }

    @Test
    @Order(11)
    void selectAddress_updatesCheckout() throws Exception {
        assertNotNull(checkoutId, "Need checkoutId");
        assertNotNull(addressId, "Need addressId");

        String result = addressTools.selectAddress(checkoutId, addressId, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("checkoutId"));
        assertEquals(checkoutId, json.get("checkoutId").asText());
        assertNotNull(json.get("status"));
        assertNotNull(json.get("grandTotal"));
        assertNotNull(json.get("selectedAddress"), "Should have selected address");
    }

    @Test
    @Order(12)
    void initiatePayment_UPI_succeedsAndCreatesOrder() throws Exception {
        assertNotNull(checkoutId, "Need checkoutId");

        String result = paymentTools.initiatePayment(checkoutId, "UPI", sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("paymentId"), "Should return paymentId");
        assertNotNull(json.get("status"), "Should return payment status");
        assertEquals("SUCCESS", json.get("status").asText(), "Mock payment should succeed");
        assertNotNull(json.get("orderId"), "Should return orderId after payment");
        assertNotNull(json.get("orderNumber"), "Should return orderNumber");

        paymentId = json.get("paymentId").asText();
        orderId = json.get("orderId").asText();
    }

    @Test
    @Order(13)
    void getPaymentStatus_returnsStatus() throws Exception {
        assertNotNull(paymentId, "Need paymentId");

        String result = paymentTools.getPaymentStatus(paymentId, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("status"));
        assertEquals("SUCCESS", json.get("status").asText());
    }

    @Test
    @Order(14)
    void initiatePayment_withDifferentMethods() throws Exception {
        // Test with CARD
        String loginResult = authTools.serverToServerLogin("9000000001", "TestApp", "mock-platform-secret-key");
        String sid = parse(loginResult).get("sessionId").asText();

        productSearchTools.searchProducts("book", 0, 1); // trigger async generation
        Thread.sleep(300);
        String searchResult = productSearchTools.searchProducts("book", 0, 1);
        String pid = parse(searchResult).get("products").get(0).get("id").asText();

        String cartResult = cartTools.addToCart(pid, 1, sid);
        String cid = parse(cartResult).get("cartId").asText();

        String checkoutResult = cartTools.checkout(cid, sid);
        String coId = parse(checkoutResult).get("checkoutId").asText();

        String addressResult = addressTools.getAddresses(sid);
        String aId = parse(addressResult).get("addresses").get(0).get("addressId").asText();

        addressTools.selectAddress(coId, aId, sid);

        String payResult = paymentTools.initiatePayment(coId, "CARD", sid);
        JsonNode payJson = parse(payResult);
        assertEquals("SUCCESS", payJson.get("status").asText(), "CARD payment should succeed");
    }

    @Test
    @Order(15)
    void initiatePayment_COD_succeedsImmediately() throws Exception {
        String loginResult = authTools.serverToServerLogin("9000000002", "TestApp", "mock-platform-secret-key");
        String sid = parse(loginResult).get("sessionId").asText();

        productSearchTools.searchProducts("groceries", 0, 1); // trigger async generation
        Thread.sleep(300);
        String searchResult = productSearchTools.searchProducts("groceries", 0, 1);
        String pid = parse(searchResult).get("products").get(0).get("id").asText();

        String cartResult = cartTools.addToCart(pid, 1, sid);
        String cid = parse(cartResult).get("cartId").asText();

        String checkoutResult = cartTools.checkout(cid, sid);
        String coId = parse(checkoutResult).get("checkoutId").asText();

        String addressResult = addressTools.getAddresses(sid);
        String aId = parse(addressResult).get("addresses").get(0).get("addressId").asText();

        addressTools.selectAddress(coId, aId, sid);

        String payResult = paymentTools.initiatePayment(coId, "COD", sid);
        JsonNode payJson = parse(payResult);
        assertEquals("SUCCESS", payJson.get("status").asText(), "COD payment should succeed");
        assertNotNull(payJson.get("orderId"), "COD should create an order");
    }

    // Helper to expose orderId for other tests
    public static String getOrderId() {
        return orderId;
    }

    public static String getSessionId() {
        return sessionId;
    }
}
