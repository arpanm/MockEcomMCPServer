package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderTools {

    private final OrderService orderService;
    private final ToolResponseHelper helper;

    @Tool(description = "Get complete order details including order status, all order items with product info and individual item statuses, plus list of shipments with tracking numbers, carrier info, and delivery status. Use orderId from initiatePayment or getOrders response. Requires valid sessionId.")
    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public String getOrderDetails(String orderId, String sessionId) {
        try {
            log.info("[Tool] getOrderDetails orderId={} session={}", orderId, sessionId);
            Order order = orderService.getOrderDetails(orderId, sessionId);
            List<OrderItem> items = orderService.getOrderItems(order);
            List<Shipment> shipments = orderService.getOrderShipments(order);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", order.getId().toString());
            m.put("orderNumber", order.getOrderNumber());
            m.put("status", order.getStatus().name());
            m.put("totalAmount", order.getTotalAmount());
            m.put("deliveryAddress", order.getDeliveryAddress());
            m.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
            m.put("items", items.stream().map(i -> {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("orderItemId", i.getId().toString());
                im.put("productId", i.getProduct() != null ? i.getProduct().getId().toString() : null);
                im.put("productTitle", i.getProduct() != null ? i.getProduct().getTitle() : "");
                im.put("quantity", i.getQuantity());
                im.put("unitPrice", i.getUnitPrice());
                im.put("totalPrice", i.getTotalPrice());
                im.put("status", i.getStatus().name());
                return im;
            }).toList());
            m.put("shipments", shipments.stream().map(s -> {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("shipmentId", s.getId().toString());
                sm.put("trackingNumber", s.getTrackingNumber());
                sm.put("carrier", s.getCarrierName());
                sm.put("status", s.getStatus().name());
                sm.put("estimatedDelivery", s.getEstimatedDeliveryDate() != null ? s.getEstimatedDeliveryDate().toString() : null);
                sm.put("pincode", s.getDeliveryPincode());
                return sm;
            }).toList());
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] getOrderDetails error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get paginated list of all orders for the authenticated customer. Returns order summaries with order number, date, total amount, status, and item count. Use page (0-based) and pageSize to paginate. Requires valid sessionId.")
    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public String getOrders(String sessionId, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] getOrders session={} page={}", sessionId, page);
            int p = page != null ? page : 0;
            int s = pageSize != null ? pageSize : 10;
            Page<Order> orders = orderService.getOrders(sessionId, p, s);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("page", p);
            result.put("totalElements", orders.getTotalElements());
            result.put("totalPages", orders.getTotalPages());
            result.put("orders", orders.getContent().stream().map(o -> {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("orderId", o.getId().toString());
                om.put("orderNumber", o.getOrderNumber());
                om.put("status", o.getStatus().name());
                om.put("totalAmount", o.getTotalAmount());
                om.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                om.put("itemCount", o.getItems() != null ? o.getItems().size() : 0);
                return om;
            }).toList());
            return helper.toJson(result);
        } catch (Exception e) {
            log.error("[Tool] getOrders error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get paginated list of all shipments for the authenticated customer across all orders. Returns shipment details with tracking number, carrier, status, estimated delivery date, and delivery pincode. Requires valid sessionId.")
    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public String getShipments(String sessionId, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] getShipments session={}", sessionId);
            int p = page != null ? page : 0;
            int s = pageSize != null ? pageSize : 10;
            Page<Shipment> shipments = orderService.getShipments(sessionId, p, s);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("page", p);
            result.put("totalElements", shipments.getTotalElements());
            result.put("shipments", shipments.getContent().stream().map(sh -> {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("shipmentId", sh.getId().toString());
                sm.put("trackingNumber", sh.getTrackingNumber());
                sm.put("carrier", sh.getCarrierName());
                sm.put("status", sh.getStatus().name());
                sm.put("estimatedDelivery", sh.getEstimatedDeliveryDate() != null ? sh.getEstimatedDeliveryDate().toString() : null);
                sm.put("pincode", sh.getDeliveryPincode());
                sm.put("orderId", sh.getOrder() != null ? sh.getOrder().getId().toString() : null);
                sm.put("orderNumber", sh.getOrder() != null ? sh.getOrder().getOrderNumber() : null);
                return sm;
            }).toList());
            return helper.toJson(result);
        } catch (Exception e) {
            log.error("[Tool] getShipments error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Cancel an order or specific order item. Provide orderId for full cancellation or orderItemId for partial cancellation (cancel single item), along with cancellation reason and sessionId. Cancellation only allowed for orders not yet shipped. Returns updated order/item status and refund information.")
    public String cancelOrder(String orderId, String orderItemId, String reason, String sessionId) {
        try {
            log.info("[Tool] cancelOrder orderId={} itemId={} session={}", orderId, orderItemId, sessionId);
            Object result = orderService.cancelOrder(orderId, orderItemId, reason, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            if (result instanceof Order o) {
                m.put("type", "ORDER"); m.put("orderId", o.getId().toString());
                m.put("orderNumber", o.getOrderNumber()); m.put("status", o.getStatus().name());
            } else if (result instanceof OrderItem oi) {
                m.put("type", "ORDER_ITEM"); m.put("orderItemId", oi.getId().toString());
                m.put("status", oi.getStatus().name());
            }
            m.put("message", "Cancellation successful. Refund will be processed in 5-7 business days.");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] cancelOrder error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Initiate a return for a delivered order item. Provide orderItemId, return reason (WRONG_ITEM/DAMAGED/NOT_AS_DESCRIBED/CHANGED_MIND/OTHER), and sessionId. Returns return request details with pickup schedule and refund timeline. Return must be initiated within return window. Only DELIVERED items can be returned.")
    public String returnOrderItem(String orderItemId, String reason, String sessionId) {
        try {
            log.info("[Tool] returnOrderItem orderItemId={} reason={} session={}", orderItemId, reason, sessionId);
            OrderItem item = orderService.returnOrderItem(orderItemId, reason, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderItemId", item.getId().toString());
            m.put("status", item.getStatus().name());
            m.put("returnReason", reason);
            m.put("pickupDate", java.time.LocalDate.now().plusDays(2).toString());
            m.put("refundTimeline", "Refund will be processed within 7-10 business days after pickup");
            m.put("message", "Return request initiated successfully. Pickup scheduled within 2-3 business days.");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] returnOrderItem error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
