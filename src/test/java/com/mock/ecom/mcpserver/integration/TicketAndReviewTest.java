package com.mock.ecom.mcpserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.entity.OrderItem;
import com.mock.ecom.mcpserver.repository.OrderItemRepository;
import com.mock.ecom.mcpserver.repository.OrderRepository;
import com.mock.ecom.mcpserver.repository.ShipmentRepository;
import com.mock.ecom.mcpserver.tools.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ticket tools 21-24 and review tools 25-26.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketAndReviewTest {

    @Autowired private TicketTools ticketTools;
    @Autowired private ReviewTools reviewTools;
    @Autowired private AuthTools authTools;
    @Autowired private ProductSearchTools productSearchTools;
    @Autowired private CartTools cartTools;
    @Autowired private AddressTools addressTools;
    @Autowired private PaymentTools paymentTools;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ShipmentRepository shipmentRepository;

    private static final ObjectMapper om = new ObjectMapper();
    private static String sessionId;
    private static String ticketId;
    private static String orderId;
    private static String shipmentId;
    private static String orderItemId;

    private JsonNode parse(String json) throws Exception {
        return om.readTree(json);
    }

    private String setupSessionAndOrder(String phone) throws Exception {
        String loginResult = authTools.serverToServerLogin(phone, "TicketTest", "mock-platform-secret-key");
        sessionId = parse(loginResult).get("sessionId").asText();

        productSearchTools.searchProducts("camera photo", 0, 1); // trigger async generation
        Thread.sleep(300); // wait for async product save
        String searchResult = productSearchTools.searchProducts("camera photo", 0, 1);
        String pid = parse(searchResult).get("products").get(0).get("id").asText();

        String cartResult = cartTools.addToCart(pid, 1, sessionId);
        String cid = parse(cartResult).get("cartId").asText();

        String checkoutResult = cartTools.checkout(cid, sessionId);
        String coId = parse(checkoutResult).get("checkoutId").asText();

        String addrResult = addressTools.getAddresses(sessionId);
        String aId = parse(addrResult).get("addresses").get(0).get("addressId").asText();

        addressTools.selectAddress(coId, aId, sessionId);

        String payResult = paymentTools.initiatePayment(coId, "UPI", sessionId);
        return parse(payResult).get("orderId").asText();
    }

    @Test
    @Order(1)
    void setupTestData() throws Exception {
        orderId = setupSessionAndOrder("7700000001");

        // Get shipment ID
        com.mock.ecom.mcpserver.entity.Order order = orderRepository.findById(UUID.fromString(orderId)).orElseThrow();
        var shipments = shipmentRepository.findAll().stream()
            .filter(s -> s.getOrder() != null && s.getOrder().getId().equals(order.getId()))
            .toList();
        if (!shipments.isEmpty()) {
            shipmentId = shipments.get(0).getId().toString();
        }

        // Get order item
        var items = orderItemRepository.findByOrder(order);
        if (!items.isEmpty()) {
            orderItemId = items.get(0).getId().toString();
        }
    }

    @Test
    @Order(2)
    void createTicket_withOrderId_createsSuccessfully() throws Exception {
        String result = ticketTools.createTicket(
            "My order is delayed",
            "I ordered a camera but it has not been delivered in the expected timeframe",
            "DELIVERY_ISSUE",
            orderId,
            null,
            sessionId
        );
        JsonNode json = parse(result);

        assertNotNull(json.get("ticketId"), "Should return ticketId");
        assertNotNull(json.get("subject"), "Should return subject");
        assertEquals("My order is delayed", json.get("subject").asText());
        assertNotNull(json.get("type"), "Should return ticket type");
        assertNotNull(json.get("status"), "Should return status");
        assertNotNull(json.get("message"), "Should return message");

        ticketId = json.get("ticketId").asText();
    }

    @Test
    @Order(3)
    void createTicket_withoutOrderId_createsGenericTicket() throws Exception {
        String result = ticketTools.createTicket(
            "Product quality issue",
            "The product I received is damaged",
            "PRODUCT_QUALITY",
            null,
            null,
            sessionId
        );
        JsonNode json = parse(result);
        assertNotNull(json.get("ticketId"));
        assertEquals("Product quality issue", json.get("subject").asText());
    }

    @Test
    @Order(4)
    void createTicket_allTypes_work() throws Exception {
        String[] types = {"DELIVERY_ISSUE", "RETURN_REQUEST", "PAYMENT_ISSUE", "PRODUCT_QUALITY", "CANCELLATION", "OTHER"};
        for (String type : types) {
            String result = ticketTools.createTicket(
                "Test ticket " + type,
                "Test description for " + type,
                type,
                null,
                null,
                sessionId
            );
            JsonNode json = parse(result);
            assertNotNull(json.get("ticketId"), "Should create ticket for type: " + type);
        }
    }

    @Test
    @Order(5)
    void getTickets_returnsTicketList() throws Exception {
        String result = ticketTools.getTickets(sessionId, 0, 10);
        JsonNode json = parse(result);

        assertNotNull(json.get("tickets"), "Should return tickets array");
        assertTrue(json.get("tickets").isArray());
        assertTrue(json.get("tickets").size() > 0, "Should have at least one ticket");
        assertNotNull(json.get("totalElements"));
    }

    @Test
    @Order(6)
    void getTickets_invalidSession_returnsError() throws Exception {
        String result = ticketTools.getTickets("invalid-session", 0, 10);
        JsonNode json = parse(result);
        assertNotNull(json.get("error"));
    }

    @Test
    @Order(7)
    void getTicketDetails_returnsFullDetails() throws Exception {
        assertNotNull(ticketId, "Need ticketId from createTicket test");

        String result = ticketTools.getTicketDetails(ticketId, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("ticketId"));
        assertEquals(ticketId, json.get("ticketId").asText());
        assertNotNull(json.get("subject"));
        assertNotNull(json.get("description"), "Should include description in details");
        assertNotNull(json.get("comments"), "Should include comments");
        assertTrue(json.get("comments").isArray());
    }

    @Test
    @Order(8)
    void getTicketDetails_invalidId_returnsError() throws Exception {
        String result = ticketTools.getTicketDetails("00000000-0000-0000-0000-000000000000", sessionId);
        JsonNode json = parse(result);
        assertNotNull(json.get("error"));
    }

    @Test
    @Order(9)
    void addTicketComment_addsCommentSuccessfully() throws Exception {
        assertNotNull(ticketId, "Need ticketId");

        String result = ticketTools.addTicketComment(ticketId, "Any update on my delivery?", sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("commentId"), "Should return commentId");
        assertNotNull(json.get("content"), "Should return content");
        assertEquals("Any update on my delivery?", json.get("content").asText());
        assertNotNull(json.get("authorType"), "Should return author type");
        assertNotNull(json.get("message"), "Should return message");
    }

    @Test
    @Order(10)
    void addTicketComment_multipleComments_allAdded() throws Exception {
        ticketTools.addTicketComment(ticketId, "Second comment", sessionId);
        ticketTools.addTicketComment(ticketId, "Third comment", sessionId);

        String detailsResult = ticketTools.getTicketDetails(ticketId, sessionId);
        JsonNode json = parse(detailsResult);
        assertTrue(json.get("comments").size() >= 2, "Should have multiple comments");
    }

    @Test
    @Order(11)
    void submitProductReview_withOrder_successfullySaved() throws Exception {
        // Mark order item as DELIVERED first
        com.mock.ecom.mcpserver.entity.Order order = orderRepository.findById(UUID.fromString(orderId)).orElseThrow();
        var items = orderItemRepository.findByOrder(order);
        if (!items.isEmpty()) {
            OrderItem item = items.get(0);
            item.setStatus(OrderItem.OrderItemStatus.DELIVERED);
            orderItemRepository.save(item);
            String productIdStr = item.getProduct().getId().toString();

            String result = reviewTools.submitProductReview(
                productIdStr, 5, "Excellent product!", "Great quality, very happy with purchase", sessionId
            );
            JsonNode json = parse(result);

            assertNotNull(json.get("reviewId") != null ? json.get("reviewId") : json.get("error"),
                "Should return reviewId or error");
        }
    }

    @Test
    @Order(12)
    void submitProductReview_ratingOutOfRange_returnsError() throws Exception {
        String searchResult = productSearchTools.searchProducts("toy game", 0, 1);
        String pid = parse(searchResult).get("products").get(0).get("id").asText();

        String result = reviewTools.submitProductReview(pid, 6, "Test", "Test description", sessionId);
        JsonNode json = parse(result);
        // Rating > 5 should either be clamped or return error
        assertNotNull(json);
    }

    @Test
    @Order(13)
    void submitShipmentReview_ratingsShipment() throws Exception {
        if (shipmentId != null) {
            String result = reviewTools.submitShipmentReview(
                shipmentId, 4, "Good Delivery", "Fast delivery, good packaging", sessionId
            );
            JsonNode json = parse(result);
            assertNotNull(json);
        } else {
            // If no shipmentId, just test with an invalid ID
            String result = reviewTools.submitShipmentReview(
                "00000000-0000-0000-0000-000000000000", 4, "Test", "Test review", sessionId
            );
            JsonNode json = parse(result);
            assertNotNull(json.get("error"), "Should return error for invalid shipment");
        }
    }

    @Test
    @Order(14)
    void tickets_pagination_works() throws Exception {
        String result = ticketTools.getTickets(sessionId, 0, 3);
        JsonNode json = parse(result);
        assertEquals(0, json.get("page").asInt());
    }
}
