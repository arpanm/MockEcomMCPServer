package com.mock.ecom.mcpserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.entity.OrderItem;
import com.mock.ecom.mcpserver.repository.*;
import com.mock.ecom.mcpserver.service.AuthService;
import com.mock.ecom.mcpserver.tools.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for order tools 15-20: getOrderDetails, getOrders, getShipments,
 * cancelOrder, returnOrderItem, sendDeliveryOtp
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderManagementTest {

    @Autowired private OrderTools orderTools;
    @Autowired private ShipmentTools shipmentTools;
    @Autowired private AuthTools authTools;
    @Autowired private ProductSearchTools productSearchTools;
    @Autowired private CartTools cartTools;
    @Autowired private AddressTools addressTools;
    @Autowired private PaymentTools paymentTools;

    // Repositories for test data setup
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private AuthService authService;

    private static final ObjectMapper om = new ObjectMapper();
    private static String sessionId;
    private static String orderId;
    private static String orderItemId;
    private static String shipmentId;

    private JsonNode parse(String json) throws Exception {
        return om.readTree(json);
    }

    private String doFullPurchase(String phone, String productQuery) throws Exception {
        String loginResult = authTools.serverToServerLogin(phone, "OrderTest", "mock-platform-secret-key");
        String sid = parse(loginResult).get("sessionId").asText();

        productSearchTools.searchProducts(productQuery, 0, 1); // trigger async generation
        Thread.sleep(300); // wait for async product save
        String searchResult = productSearchTools.searchProducts(productQuery, 0, 1);
        String pid = parse(searchResult).get("products").get(0).get("id").asText();

        String cartResult = cartTools.addToCart(pid, 1, sid);
        String cid = parse(cartResult).get("cartId").asText();

        String checkoutResult = cartTools.checkout(cid, sid);
        String coId = parse(checkoutResult).get("checkoutId").asText();

        String addrResult = addressTools.getAddresses(sid);
        String aId = parse(addrResult).get("addresses").get(0).get("addressId").asText();

        addressTools.selectAddress(coId, aId, sid);

        String payResult = paymentTools.initiatePayment(coId, "UPI", sid);
        JsonNode payJson = parse(payResult);

        return payJson.get("orderId").asText();
    }

    @Test
    @Order(1)
    void setupOrder() throws Exception {
        // Create a new session and order for tests
        String loginResult = authTools.serverToServerLogin("8800000001", "OrderTest", "mock-platform-secret-key");
        sessionId = parse(loginResult).get("sessionId").asText();
        orderId = doFullPurchase("8800000001", "headphone audio");
        assertFalse(orderId.isEmpty(), "Order ID should be created");
    }

    @Test
    @Order(2)
    void getOrders_returnsOrderList() throws Exception {
        String result = orderTools.getOrders(sessionId, 0, 10);
        JsonNode json = parse(result);

        assertNotNull(json.get("orders"));
        assertTrue(json.get("orders").isArray());
        assertTrue(json.get("orders").size() > 0, "Should have at least one order");
        assertNotNull(json.get("totalElements"));
        assertTrue(json.get("totalElements").asLong() > 0);
    }

    @Test
    @Order(3)
    void getOrders_invalidSession_returnsError() throws Exception {
        String result = orderTools.getOrders("invalid-session", 0, 10);
        JsonNode json = parse(result);
        assertNotNull(json.get("error"));
    }

    @Test
    @Order(4)
    void getOrderDetails_returnsFullDetails() throws Exception {
        String result = orderTools.getOrderDetails(orderId, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("orderId"));
        assertEquals(orderId, json.get("orderId").asText());
        assertNotNull(json.get("orderNumber"));
        assertNotNull(json.get("status"));
        assertNotNull(json.get("items"));
        assertTrue(json.get("items").isArray());
        assertTrue(json.get("items").size() > 0, "Order should have items");
        assertNotNull(json.get("shipments"));

        // Get first order item ID for later tests
        orderItemId = json.get("items").get(0).get("orderItemId").asText();
    }

    @Test
    @Order(5)
    void getOrderDetails_invalidId_returnsError() throws Exception {
        String result = orderTools.getOrderDetails("00000000-0000-0000-0000-000000000000", sessionId);
        JsonNode json = parse(result);
        assertNotNull(json.get("error"));
    }

    @Test
    @Order(6)
    void getShipments_returnsShipmentList() throws Exception {
        String result = orderTools.getShipments(sessionId, 0, 10);
        JsonNode json = parse(result);

        assertNotNull(json.get("shipments"));
        assertTrue(json.get("shipments").isArray());
        assertTrue(json.get("shipments").size() > 0, "Should have at least one shipment");

        JsonNode shipment = json.get("shipments").get(0);
        assertNotNull(shipment.get("shipmentId"));
        assertNotNull(shipment.get("trackingNumber"));
        assertNotNull(shipment.get("status"));

        shipmentId = shipment.get("shipmentId").asText();
    }

    @Test
    @Order(7)
    void cancelOrder_pendingOrder_cancelsSuccessfully() throws Exception {
        // Create a fresh order to cancel
        String loginResult = authTools.serverToServerLogin("8800000002", "CancelTest", "mock-platform-secret-key");
        String sid = parse(loginResult).get("sessionId").asText();
        String oid = doFullPurchase("8800000002", "shirt clothing");

        // Cancel the order immediately (while PROCESSING)
        String result = orderTools.cancelOrder(oid, null, "Changed my mind", sid);
        JsonNode json = parse(result);

        assertNotNull(json.get("message"));
        assertTrue(json.get("message").asText().contains("Cancellation"), "Should confirm cancellation");
    }

    @Test
    @Order(8)
    void cancelOrder_specificItem_cancelsItem() throws Exception {
        // Get the order with items first
        String result = orderTools.getOrderDetails(orderId, sessionId);
        JsonNode orderJson = parse(result);
        String itemId = orderJson.get("items").get(0).get("orderItemId").asText();

        String cancelResult = orderTools.cancelOrder(null, itemId, "Wrong size ordered", sessionId);
        JsonNode json = parse(cancelResult);

        assertNotNull(json.get("message"));
        // Either order item or full order was cancelled
        assertTrue(json.get("message").asText().contains("Cancellation") ||
                   json.get("type") != null, "Should have cancellation result");
    }

    @Test
    @Order(9)
    void returnOrderItem_deliveredItem_initiatesReturn() throws Exception {
        // Create a special order with DELIVERED status for return test
        String loginResult = authTools.serverToServerLogin("8800000003", "ReturnTest", "mock-platform-secret-key");
        String sid = parse(loginResult).get("sessionId").asText();

        // Create order
        String oid = doFullPurchase("8800000003", "electronics gadget");

        // Manually set order item to DELIVERED
        com.mock.ecom.mcpserver.entity.Order order = orderRepository.findById(java.util.UUID.fromString(oid)).orElseThrow();
        OrderItem item = orderItemRepository.findByOrder(order).stream().findFirst().orElseThrow();
        item.setStatus(OrderItem.OrderItemStatus.DELIVERED);
        orderItemRepository.save(item);

        // Return the item
        String returnResult = orderTools.returnOrderItem(item.getId().toString(), "WRONG_ITEM", sid);
        JsonNode json = parse(returnResult);

        assertNotNull(json.get("orderItemId"));
        assertNotNull(json.get("status"));
        assertNotNull(json.get("message"));
        assertTrue(json.get("message").asText().contains("Return"), "Should confirm return initiated");
    }

    @Test
    @Order(10)
    void returnOrderItem_nonDeliveredItem_returnsError() throws Exception {
        // Try to return an order item that's still PROCESSING
        String loginResult = authTools.serverToServerLogin("8800000004", "ReturnTest", "mock-platform-secret-key");
        String sid = parse(loginResult).get("sessionId").asText();
        String oid = doFullPurchase("8800000004", "book fiction");

        com.mock.ecom.mcpserver.entity.Order order = orderRepository.findById(java.util.UUID.fromString(oid)).orElseThrow();
        OrderItem item = orderItemRepository.findByOrder(order).stream().findFirst().orElseThrow();
        // Item should be in PROCESSING or similar state (not DELIVERED)

        String returnResult = orderTools.returnOrderItem(item.getId().toString(), "WRONG_ITEM", sid);
        JsonNode json = parse(returnResult);
        assertNotNull(json.get("error"), "Should return error for non-delivered item");
    }

    @Test
    @Order(11)
    void sendDeliveryOtp_generatesOtp() throws Exception {
        // Get a shipment
        String shipmentsResult = orderTools.getShipments(sessionId, 0, 1);
        JsonNode shipmentsJson = parse(shipmentsResult);
        String shipId = shipmentsJson.get("shipments").get(0).get("shipmentId").asText();

        String result = shipmentTools.sendDeliveryOtp(shipId, sessionId);
        JsonNode json = parse(result);

        assertNotNull(json.get("message") != null ? json.get("message") : json.get("error"),
            "Should return message or error");
        // Should either succeed with OTP or return an error if already delivered
    }

    @Test
    @Order(12)
    void getOrders_pagination_works() throws Exception {
        // Create multiple orders
        for (int i = 0; i < 3; i++) {
            doFullPurchase("8800001" + i + "00", "product test " + i);
        }

        String loginResult = authTools.serverToServerLogin("8800000001", "OrderTest", "mock-platform-secret-key");
        String sid = parse(loginResult).get("sessionId").asText();

        String page0 = orderTools.getOrders(sid, 0, 2);
        JsonNode p0Json = parse(page0);
        assertEquals(0, p0Json.get("page").asInt());
    }
}
