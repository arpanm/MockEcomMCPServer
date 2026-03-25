package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuthService authService;

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Order getOrderDetails(String orderId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return orderRepository.findById(UUID.fromString(orderId))
            .filter(o -> o.getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Page<Order> getOrders(String sessionId, int page, int pageSize) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Page<Shipment> getShipments(String sessionId, int page, int pageSize) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return shipmentRepository.findByCustomer(customer, PageRequest.of(page, pageSize));
    }

    @Transactional
    public Object cancelOrder(String orderId, String orderItemId, String reason, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        if (orderItemId != null && !orderItemId.isBlank()) {
            OrderItem item = orderItemRepository.findById(UUID.fromString(orderItemId))
                .filter(i -> i.getOrder().getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Order item not found"));
            if (item.getStatus() == OrderItem.OrderItemStatus.SHIPPED || item.getStatus() == OrderItem.OrderItemStatus.DELIVERED)
                throw new IllegalArgumentException("Cannot cancel shipped/delivered item");
            item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
            return orderItemRepository.save(item);
        } else {
            Order order = getOrderDetails(orderId, sessionId);
            if (order.getStatus() == Order.OrderStatus.SHIPPED || order.getStatus() == Order.OrderStatus.DELIVERED)
                throw new IllegalArgumentException("Cannot cancel shipped/delivered order");
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderItemRepository.findByOrder(order).forEach(i -> { i.setStatus(OrderItem.OrderItemStatus.CANCELLED); orderItemRepository.save(i); });
            return orderRepository.save(order);
        }
    }

    @Transactional
    public OrderItem returnOrderItem(String orderItemId, String reason, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        OrderItem item = orderItemRepository.findById(UUID.fromString(orderItemId))
            .filter(i -> i.getOrder().getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Order item not found"));
        if (item.getStatus() != OrderItem.OrderItemStatus.DELIVERED)
            throw new IllegalArgumentException("Only delivered items can be returned");
        item.setStatus(OrderItem.OrderItemStatus.RETURN_INITIATED);
        return orderItemRepository.save(item);
    }

    @Transactional
    public String sendDeliveryOtp(String shipmentId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        Shipment shipment = shipmentRepository.findById(UUID.fromString(shipmentId))
            .filter(s -> s.getOrder().getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
        String otp = String.format("%06d", Math.abs(System.currentTimeMillis() % 1000000));
        shipment.setDeliveryOtp(otp);
        shipmentRepository.save(shipment);
        return otp;
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public List<OrderItem> getOrderItems(Order order) {
        return orderItemRepository.findByOrder(order);
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public List<Shipment> getOrderShipments(Order order) {
        return shipmentRepository.findByOrder(order);
    }
}
