package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShipmentTools {

    private final OrderService orderService;
    private final ToolResponseHelper helper;

    @Tool(description = "Send a delivery OTP to the customer's registered mobile number for a shipment. This OTP is required for contactless delivery verification - the delivery agent will ask the customer for this OTP to confirm delivery. Provide shipmentId (from getOrderDetails or getShipments) and sessionId. OTP is valid for 10 minutes.")
    public String sendDeliveryOtp(String shipmentId, String sessionId) {
        try {
            log.info("[Tool] sendDeliveryOtp shipmentId={} session={}", shipmentId, sessionId);
            String otp = orderService.sendDeliveryOtp(shipmentId, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shipmentId", shipmentId);
            m.put("status", "OTP_SENT");
            m.put("otpLength", 6);
            m.put("validForMinutes", 10);
            m.put("message", "OTP has been sent to your registered mobile number. Please share it with the delivery agent.");
            // In production, OTP is only sent via SMS - NOT returned in response
            // For mock/testing only:
            m.put("mockOtp", otp);
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] sendDeliveryOtp error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
