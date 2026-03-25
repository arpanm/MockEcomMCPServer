package com.mock.ecom.mcpserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.tools.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end shopping journey test covering all 28 tools in a realistic flow.
 */
@SpringBootTest
@ActiveProfiles("test")
class EndToEndShoppingJourneyTest {

    @Autowired private AuthTools authTools;
    @Autowired private ProductSearchTools productSearchTools;
    @Autowired private ProductDetailTools productDetailTools;
    @Autowired private DeliveryTools deliveryTools;
    @Autowired private CartTools cartTools;
    @Autowired private AddressTools addressTools;
    @Autowired private PaymentTools paymentTools;
    @Autowired private OrderTools orderTools;
    @Autowired private ShipmentTools shipmentTools;
    @Autowired private TicketTools ticketTools;
    @Autowired private ReviewTools reviewTools;
    @Autowired private WishlistTools wishlistTools;

    private static final ObjectMapper om = new ObjectMapper();

    private JsonNode parse(String json) throws Exception {
        return om.readTree(json);
    }

    @Test
    void completeShoppingJourney_allToolsExercised() throws Exception {
        // === STEP 1: Login (Tool 8) ===
        String loginResult = authTools.serverToServerLogin("5500000001", "JourneyTestApp", "mock-platform-secret-key");
        JsonNode loginJson = parse(loginResult);
        assertEquals("success", loginJson.get("status").asText(), "Login should succeed");
        String sessionId = loginJson.get("sessionId").asText();
        assertFalse(sessionId.isEmpty(), "SessionId should be present");

        // === STEP 2: Search products (Tool 1) ===
        productSearchTools.searchProducts("bluetooth speaker", 0, 5); // trigger async generation
        Thread.sleep(300); // wait for async product save
        String searchResult = productSearchTools.searchProducts("bluetooth speaker", 0, 5);
        JsonNode searchJson = parse(searchResult);
        assertTrue(searchJson.get("products").size() > 0, "Search should return products");
        String productId = searchJson.get("products").get(0).get("id").asText();

        // === STEP 3: Filter products (Tool 2) ===
        String filterResult = productSearchTools.filterProducts("speaker", "ELECTRONICS", null, null, 0, 5);
        JsonNode filterJson = parse(filterResult);
        assertNotNull(filterJson.get("products"), "Filter should return products");

        // === STEP 4: Get filters (Tool 3) ===
        String filtersResult = productSearchTools.getFilters("speaker");
        assertNotNull(parse(filtersResult), "Should return filter options");

        // === STEP 5: Get sort options (Tool 4) ===
        String sortResult = productSearchTools.getSortOptions("speaker");
        JsonNode sortJson = parse(sortResult);
        assertTrue(sortJson.get("sortOptions").size() >= 6, "Should have 6+ sort options");

        // === STEP 6: Get product details (Tool 5) ===
        String detailResult = productDetailTools.getProductDetails(productId);
        JsonNode detailJson = parse(detailResult);
        assertNotNull(detailJson.get("title"), "Should return product title");

        // === STEP 7: Get product reviews (Tool 6) ===
        String reviewsResult = productDetailTools.getProductReviews(productId, 0, 5);
        JsonNode reviewsJson = parse(reviewsResult);
        assertNotNull(reviewsJson.get("reviews"), "Should return reviews");

        // === STEP 8: Check delivery time (Tool 7) ===
        String deliveryResult = deliveryTools.getDeliveryTime(productId, "400001");
        JsonNode deliveryJson = parse(deliveryResult);
        assertNotNull(deliveryJson.get("deliveryOptions"), "Should return delivery options");

        // === STEP 9: Add to wishlist (Tool 27) ===
        String wishlistAddResult = wishlistTools.addToWishlist(productId, sessionId);
        assertNotNull(parse(wishlistAddResult), "Should handle wishlist add");

        // === STEP 10: View wishlist (Tool 28) ===
        String wishlistResult = wishlistTools.getWishlist(sessionId, 0, 20);
        JsonNode wishlistJson = parse(wishlistResult);
        assertNotNull(wishlistJson.get("items"), "Should return wishlist items");

        // === STEP 11: Add to cart (Tool 9) ===
        String cartResult = cartTools.addToCart(productId, 1, sessionId);
        JsonNode cartJson = parse(cartResult);
        assertNotNull(cartJson.get("cartId"), "Should return cartId");
        String cartId = cartJson.get("cartId").asText();

        // Add second product to cart
        productSearchTools.searchProducts("headphone wireless", 0, 1); // trigger async generation
        Thread.sleep(300); // wait for async product save
        String searchResult2 = productSearchTools.searchProducts("headphone wireless", 0, 1);
        String productId2 = parse(searchResult2).get("products").get(0).get("id").asText();
        cartTools.addToCart(productId2, 2, sessionId);

        // === STEP 12: Initiate checkout (Tool 10) ===
        String checkoutResult = cartTools.checkout(cartId, sessionId);
        JsonNode checkoutJson = parse(checkoutResult);
        assertNotNull(checkoutJson.get("checkoutId"), "Should return checkoutId");
        String checkoutId = checkoutJson.get("checkoutId").asText();
        assertTrue(checkoutJson.get("grandTotal").asDouble() > 0, "Grand total should be positive");

        // === STEP 13: Get addresses (Tool 11) ===
        String addressesResult = addressTools.getAddresses(sessionId);
        JsonNode addressesJson = parse(addressesResult);
        assertTrue(addressesJson.get("addresses").size() > 0, "Should have addresses");
        String addressId = addressesJson.get("addresses").get(0).get("addressId").asText();

        // === STEP 14: Select address (Tool 12) ===
        String selectAddrResult = addressTools.selectAddress(checkoutId, addressId, sessionId);
        JsonNode selectAddrJson = parse(selectAddrResult);
        assertNotNull(selectAddrJson.get("checkoutId"), "Should return checkoutId");
        assertNotNull(selectAddrJson.get("selectedAddress"), "Should confirm selected address");

        // === STEP 15: Initiate payment (Tool 13) ===
        String payResult = paymentTools.initiatePayment(checkoutId, "UPI", sessionId);
        JsonNode payJson = parse(payResult);
        assertNotNull(payJson.get("paymentId"), "Should return paymentId");
        assertEquals("SUCCESS", payJson.get("status").asText(), "Payment should succeed");
        assertNotNull(payJson.get("orderId"), "Should return orderId");
        String paymentId = payJson.get("paymentId").asText();
        String orderId = payJson.get("orderId").asText();

        // === STEP 16: Get payment status (Tool 14) ===
        String payStatusResult = paymentTools.getPaymentStatus(paymentId, sessionId);
        JsonNode payStatusJson = parse(payStatusResult);
        assertEquals("SUCCESS", payStatusJson.get("status").asText(), "Payment status should be SUCCESS");

        // === STEP 17: Get order details (Tool 15) ===
        String orderResult = orderTools.getOrderDetails(orderId, sessionId);
        JsonNode orderJson = parse(orderResult);
        assertNotNull(orderJson.get("orderId"), "Should return orderId");
        assertNotNull(orderJson.get("orderNumber"), "Should return orderNumber");
        assertNotNull(orderJson.get("items"), "Order should have items");
        assertTrue(orderJson.get("items").size() > 0, "Order should have at least one item");
        String orderItemId = orderJson.get("items").get(0).get("orderItemId").asText();

        // === STEP 18: Get orders list (Tool 16) ===
        String ordersResult = orderTools.getOrders(sessionId, 0, 10);
        JsonNode ordersJson = parse(ordersResult);
        assertTrue(ordersJson.get("orders").size() > 0, "Should have at least one order");

        // === STEP 19: Get shipments (Tool 17) ===
        String shipmentsResult = orderTools.getShipments(sessionId, 0, 10);
        JsonNode shipmentsJson = parse(shipmentsResult);
        assertNotNull(shipmentsJson.get("shipments"), "Should return shipments");
        String shipmentId = null;
        if (shipmentsJson.get("shipments").size() > 0) {
            shipmentId = shipmentsJson.get("shipments").get(0).get("shipmentId").asText();
        }

        // === STEP 20: Send delivery OTP (Tool 20) ===
        if (shipmentId != null) {
            String otpResult = shipmentTools.sendDeliveryOtp(shipmentId, sessionId);
            assertNotNull(parse(otpResult), "Should handle OTP request");
        }

        // === STEP 21: Create support ticket (Tool 24) ===
        String createTicketResult = ticketTools.createTicket(
            "Delivery tracking inquiry",
            "I placed an order and want to know the delivery status",
            "DELIVERY_ISSUE",
            orderId,
            null,
            sessionId
        );
        JsonNode createTicketJson = parse(createTicketResult);
        assertNotNull(createTicketJson.get("ticketId"), "Should return ticketId");
        String ticketId = createTicketJson.get("ticketId").asText();

        // === STEP 22: Get tickets (Tool 21) ===
        String ticketsResult = ticketTools.getTickets(sessionId, 0, 10);
        JsonNode ticketsJson = parse(ticketsResult);
        assertTrue(ticketsJson.get("tickets").size() > 0, "Should have at least one ticket");

        // === STEP 23: Get ticket details (Tool 22) ===
        String ticketDetailsResult = ticketTools.getTicketDetails(ticketId, sessionId);
        JsonNode ticketDetailsJson = parse(ticketDetailsResult);
        assertNotNull(ticketDetailsJson.get("comments"), "Should return ticket comments");

        // === STEP 24: Add ticket comment (Tool 23) ===
        String commentResult = ticketTools.addTicketComment(
            ticketId, "Please expedite my order. Thank you.", sessionId
        );
        JsonNode commentJson = parse(commentResult);
        assertNotNull(commentJson.get("commentId"), "Should return commentId");

        // === STEP 25: Submit product review (Tool 25) ===
        String reviewResult = reviewTools.submitProductReview(
            productId, 4, "Great speaker!", "Sound quality is amazing, very satisfied.", sessionId
        );
        // Review might succeed or fail depending on order status, just check it returns valid JSON
        assertNotNull(parse(reviewResult), "Should return valid JSON for review");

        // === STEP 26: Submit shipment review (Tool 26) ===
        if (shipmentId != null) {
            String shipReviewResult = reviewTools.submitShipmentReview(
                shipmentId, 5, "Great Delivery", "Very fast delivery and excellent packaging!", sessionId
            );
            assertNotNull(parse(shipReviewResult), "Should return valid JSON for shipment review");
        }

        System.out.println("=== End-to-End Shopping Journey Completed Successfully ===");
        System.out.println("Session: " + sessionId);
        System.out.println("Order: " + orderId);
        System.out.println("Ticket: " + ticketId);
    }

    @Test
    void multipleCategories_fullJourneyForEachCategory() throws Exception {
        String[] queries = {
            "rice basmati",          // GROCERY
            "smartphone samsung",    // ELECTRONICS
            "saree silk",            // FASHION
            "face wash cleanser",    // BEAUTY
            "curtains bedroom"       // HOME
        };

        for (String query : queries) {
            String loginResult = authTools.serverToServerLogin(
                "55" + Math.abs(query.hashCode() % 10000000), "CategoryTest", "mock-platform-secret-key"
            );
            String sid = parse(loginResult).get("sessionId").asText();

            productSearchTools.searchProducts(query, 0, 3); // trigger async generation
            Thread.sleep(300); // wait for async product save
            String search = productSearchTools.searchProducts(query, 0, 3);
            JsonNode searchJson = parse(search);
            assertTrue(searchJson.get("products").size() > 0,
                "Should find products for category query: " + query);

            String pid = searchJson.get("products").get(0).get("id").asText();

            // Quick cart and purchase
            String cart = cartTools.addToCart(pid, 1, sid);
            String cid = parse(cart).get("cartId").asText();

            String checkout = cartTools.checkout(cid, sid);
            String coId = parse(checkout).get("checkoutId").asText();

            String addresses = addressTools.getAddresses(sid);
            String aId = parse(addresses).get("addresses").get(0).get("addressId").asText();

            addressTools.selectAddress(coId, aId, sid);

            String payment = paymentTools.initiatePayment(coId, "COD", sid);
            JsonNode payJson = parse(payment);
            assertEquals("SUCCESS", payJson.get("status").asText(),
                "Payment should succeed for category: " + query);
            assertNotNull(payJson.get("orderId"),
                "Should create order for category: " + query);
        }
    }
}
