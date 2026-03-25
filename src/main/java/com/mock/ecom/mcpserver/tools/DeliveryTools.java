package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryTools {

    private final DeliveryService deliveryService;
    private final ToolResponseHelper helper;

    @Tool(description = "Get estimated delivery time and options for a product to a specific 6-digit Indian pincode. Returns standard and express delivery options with estimated dates, charges, and carrier info. Metro cities (Delhi, Mumbai, Bangalore, Chennai, Kolkata, Hyderabad) get 1-2 day delivery. Tier-2 cities get 3-4 days. Other areas get 5-7 days.")
    public String getDeliveryTime(String productId, String pincode) {
        try {
            log.info("[Tool] getDeliveryTime productId={} pincode={}", productId, pincode);
            Map<String, Object> result = deliveryService.getDeliveryTime(productId, pincode);
            return helper.toJson(result);
        } catch (Exception e) {
            log.error("[Tool] getDeliveryTime error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
