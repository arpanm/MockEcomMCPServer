package com.mock.ecom.mcpserver.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ToolResponseHelper {

    private final ObjectMapper objectMapper;

    public String success(Object data) {
        try {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("status", "success");
            r.put("data", data);
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"Serialization failed\"}";
        }
    }

    public String error(String message) {
        return String.format("{\"status\":\"error\",\"error\":\"%s\"}", message.replace("\"", "'"));
    }

    public String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return error("Serialization failed"); }
    }

    public Map<String, Object> productToMap(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId() != null ? p.getId().toString() : null);
        m.put("title", p.getTitle());
        m.put("description", p.getDescription());
        m.put("category", p.getCategory());
        m.put("subCategory", p.getSubCategory());
        m.put("brand", p.getBrand());
        m.put("model", p.getModel());
        m.put("imageUrl", p.getImageUrl());
        m.put("price", p.getPrice());
        m.put("mrp", p.getMrp());
        m.put("currency", p.getCurrency());
        if (p.getMrp() != null && p.getMrp().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal disc = p.getMrp().subtract(p.getPrice())
                .divide(p.getMrp(), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
            m.put("discountPercentage", disc);
        }
        m.put("averageRating", p.getAverageRating());
        m.put("reviewCount", p.getReviewCount());
        m.put("stockQuantity", p.getStockQuantity());
        m.put("inStock", p.getStockQuantity() != null && p.getStockQuantity() > 0);
        return m;
    }
}
