package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.Session;
import com.mock.ecom.mcpserver.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthTools {

    private final AuthService authService;
    private final ToolResponseHelper helper;

    @Tool(description = "Server-to-server login for ecommerce platforms. The AI chatbot server calls this with the customer's phone number and platform credentials to obtain a session ID. This session ID must then be passed to all subsequent APIs that require authentication (cart, orders, wishlist, etc.). The session is valid for 24 hours. Platform secret for mock: 'mock-platform-secret-key'.")
    public String serverToServerLogin(String phoneNumber, String platform, String secret) {
        try {
            log.info("[Tool] serverToServerLogin phone={} platform={}", phoneNumber, platform);
            Session session = authService.serverToServerLogin(phoneNumber, platform, secret);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sessionId", session.getId().toString());
            result.put("customerId", session.getCustomer().getId().toString());
            result.put("phoneNumber", session.getPhoneNumber());
            result.put("platform", session.getPlatform());
            result.put("expiresAt", session.getExpiresAt().toString());
            result.put("status", "success");
            result.put("message", "Login successful. Use sessionId for authenticated API calls.");
            return helper.toJson(result);
        } catch (Exception e) {
            log.error("[Tool] serverToServerLogin error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
