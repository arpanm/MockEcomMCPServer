package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentTools {

    private final PaymentService paymentService;
    private final ToolResponseHelper helper;

    @Tool(description = "Initiate payment for a checkout. Provide checkoutId (from selectAddress), paymentMethod (UPI/CARD/NETBANKING/COD/WALLET), and sessionId. Returns paymentId and payment details. Mock payments auto-succeed. For COD, order is confirmed immediately. After payment, use getPaymentStatus to get the orderId for tracking.")
    public String initiatePayment(String checkoutId, String paymentMethod, String sessionId) {
        try {
            log.info("[Tool] initiatePayment checkoutId={} method={} session={}", checkoutId, paymentMethod, sessionId);
            var payment = paymentService.initiatePayment(checkoutId, paymentMethod, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paymentId", payment.getId().toString());
            m.put("status", payment.getStatus().name());
            m.put("amount", payment.getAmount());
            m.put("paymentMethod", payment.getPaymentMethod());
            m.put("transactionId", payment.getTransactionId());
            if (payment.getOrder() != null) {
                m.put("orderId", payment.getOrder().getId().toString());
                m.put("orderNumber", payment.getOrder().getOrderNumber());
            }
            m.put("message", "Payment successful! Your order has been placed.");
            m.put("nextStep", "Use orderId to track your order with getOrderDetails");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] initiatePayment error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get payment status and associated order ID for a payment. Provide paymentId (from initiatePayment response) and sessionId. Returns payment status (PENDING/PROCESSING/SUCCESS/FAILED), transaction ID, and if payment succeeded, the orderId for order tracking.")
    public String getPaymentStatus(String paymentId, String sessionId) {
        try {
            log.info("[Tool] getPaymentStatus paymentId={} session={}", paymentId, sessionId);
            Map<String, Object> result = paymentService.getPaymentStatus(paymentId, sessionId);
            return helper.toJson(result);
        } catch (Exception e) {
            log.error("[Tool] getPaymentStatus error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
