package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CheckoutRepository checkoutRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final CartItemRepository cartItemRepository;
    private final CheckoutService checkoutService;
    private final AuthService authService;

    @Transactional
    public Payment initiatePayment(String checkoutId, String paymentMethod, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        Checkout checkout = checkoutService.getCheckout(UUID.fromString(checkoutId), sessionId);
        if (checkout.getStatus() != Checkout.CheckoutStatus.ADDRESS_SELECTED)
            throw new IllegalArgumentException("Please select address before payment");
        checkout.setStatus(Checkout.CheckoutStatus.PAYMENT_INITIATED);
        checkoutRepository.save(checkout);
        Payment payment = Payment.builder()
            .checkout(checkout).customer(customer)
            .amount(checkout.getGrandTotal())
            .status(Payment.PaymentStatus.PROCESSING)
            .paymentMethod(paymentMethod)
            .transactionId("TXN" + System.currentTimeMillis())
            .build();
        payment = paymentRepository.save(payment);
        // Mock: COD always succeeds, others simulate success
        Order order = createOrderFromCheckout(checkout, payment);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setOrder(order);
        paymentRepository.save(payment);
        checkout.setStatus(Checkout.CheckoutStatus.COMPLETED);
        checkoutRepository.save(checkout);
        return payment;
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Map<String, Object> getPaymentStatus(String paymentId, String sessionId) {
        authService.validateSession(sessionId);
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", payment.getId().toString());
        result.put("status", payment.getStatus().name());
        result.put("amount", payment.getAmount());
        result.put("paymentMethod", payment.getPaymentMethod());
        result.put("transactionId", payment.getTransactionId());
        if (payment.getOrder() != null) result.put("orderId", payment.getOrder().getId().toString());
        return result;
    }

    private Order createOrderFromCheckout(Checkout checkout, Payment payment) {
        String orderNum = "ORD" + System.currentTimeMillis();
        Address addr = checkout.getAddress();
        String addrSnap = addr != null ? addr.getAddressLine1() + ", " + addr.getCity() + " " + addr.getPincode() : "";
        Order order = orderRepository.save(Order.builder()
            .checkout(checkout).customer(checkout.getCustomer())
            .orderNumber(orderNum).status(Order.OrderStatus.PLACED)
            .totalAmount(checkout.getGrandTotal())
            .deliveryAddress(addrSnap).build());
        List<CartItem> cartItems = cartItemRepository.findByCart(checkout.getCart());
        Shipment shipment = shipmentRepository.save(Shipment.builder()
            .order(order)
            .trackingNumber("TRK" + System.currentTimeMillis())
            .carrierName(pickCarrier(addr != null ? addr.getPincode() : "400001"))
            .status(Shipment.ShipmentStatus.PENDING)
            .estimatedDeliveryDate(estimateDelivery(addr != null ? addr.getPincode() : "400001"))
            .deliveryPincode(addr != null ? addr.getPincode() : "400001")
            .build());
        for (CartItem ci : cartItems) {
            orderItemRepository.save(OrderItem.builder()
                .order(order).product(ci.getProduct())
                .quantity(ci.getQuantity()).unitPrice(ci.getUnitPrice())
                .totalPrice(ci.getTotalPrice()).status(OrderItem.OrderItemStatus.ORDERED)
                .shipment(shipment).build());
        }
        return order;
    }

    private String pickCarrier(String pincode) {
        String[] carriers = {"Delhivery","BlueDart","DTDC","Ekart","Amazon Logistics"};
        return carriers[Math.abs(pincode.hashCode()) % carriers.length];
    }

    private LocalDate estimateDelivery(String pincode) {
        Set<String> metro = Set.of("110","400","560","600","700","500");
        String prefix = pincode.length() >= 3 ? pincode.substring(0, 3) : "999";
        int days = metro.contains(prefix) ? 2 : pincode.startsWith("5") || pincode.startsWith("3") ? 4 : 6;
        return LocalDate.now().plusDays(days);
    }
}
