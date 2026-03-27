package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.WishlistItem;
import com.mock.ecom.mcpserver.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WishlistTools {

    private final WishlistService wishlistService;
    private final ToolResponseHelper helper;

    @Tool(description = "Add a product to the customer's wishlist for future purchase. If product already in wishlist, returns the existing entry (no duplicates). Requires productId and valid sessionId. Returns wishlist item details with product summary including current price and availability.")
    public String addToWishlist(String productId, String sessionId) {
        try {
            log.info("[Tool] addToWishlist productId={} session={}", productId, sessionId);
            WishlistItem item = wishlistService.addToWishlist(productId, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("wishlistItemId", item.getId().toString());
            m.put("productId", productId);
            if (item.getProduct() != null) {
                m.put("productTitle", item.getProduct().getTitle());
                m.put("price", item.getProduct().getPrice());
                m.put("imageUrl", item.getProduct().getImageUrl());
                m.put("inStock", item.getProduct().getStockQuantity() != null && item.getProduct().getStockQuantity() > 0);
            }
            m.put("addedAt", item.getAddedAt() != null ? item.getAddedAt().toString() : null);
            m.put("message", "Product added to wishlist successfully.");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] addToWishlist error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get paginated list of all products in the customer's wishlist. Returns wishlist items with full product details including current price, MRP, discount, availability status, and when the item was added. Requires valid sessionId.")
    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public String getWishlist(String sessionId, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] getWishlist session={} page={}", sessionId, page);
            int p = page != null ? page : 0;
            int s = pageSize != null ? pageSize : 10;
            Page<WishlistItem> items = wishlistService.getWishlist(sessionId, p, s);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("page", p);
            result.put("totalElements", items.getTotalElements());
            result.put("totalPages", items.getTotalPages());
            result.put("items", items.getContent().stream().map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("wishlistItemId", i.getId().toString());
                m.put("addedAt", i.getAddedAt() != null ? i.getAddedAt().toString() : null);
                if (i.getProduct() != null) m.put("product", helper.productToMap(i.getProduct()));
                return m;
            }).toList());
            return helper.toJson(result);
        } catch (Exception e) {
            log.error("[Tool] getWishlist error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
