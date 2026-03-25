package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.Cart;
import com.mock.ecom.mcpserver.entity.CartItem;
import com.mock.ecom.mcpserver.entity.Checkout;
import com.mock.ecom.mcpserver.repository.CartItemRepository;
import com.mock.ecom.mcpserver.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartTools {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final ToolResponseHelper helper;

    @Tool(description = "Add a product to the customer's shopping cart. Requires a valid sessionId obtained from serverToServerLogin. Provide product ID and quantity. If product already in cart, increases quantity. Returns updated cart with all items, quantities, unit prices, and total amount. First call serverToServerLogin to get sessionId.")
    public String addToCart(String productId, Integer quantity, String sessionId) {
        try {
            log.info("[Tool] addToCart productId={} qty={} session={}", productId, quantity, sessionId);
            int qty = quantity != null ? quantity : 1;
            Cart cart = cartService.addToCart(productId, qty, sessionId);
            return helper.toJson(cartToMap(cart));
        } catch (Exception e) {
            log.error("[Tool] addToCart error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Initiate checkout for the customer's active cart. Provide cart ID (from addToCart response) and sessionId. Creates a checkout session with order summary, subtotal, and estimated totals. After checkout, use getAddresses to pick delivery address, then selectAddress, then initiatePayment.")
    public String checkout(String cartId, String sessionId) {
        try {
            log.info("[Tool] checkout cartId={} session={}", cartId, sessionId);
            Checkout co = cartService.initiateCheckout(cartId, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("checkoutId", co.getId().toString());
            m.put("status", co.getStatus().name());
            m.put("totalAmount", co.getTotalAmount());
            m.put("deliveryCharge", co.getDeliveryCharge());
            m.put("grandTotal", co.getGrandTotal());
            m.put("nextStep", "Call getAddresses to get saved addresses, then selectAddress to choose delivery address");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] checkout error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    private Map<String, Object> cartToMap(Cart cart) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cartId", cart.getId().toString());
        m.put("status", cart.getStatus().name());
        m.put("totalAmount", cart.getTotalAmount());
        List<CartItem> items = cartItemRepository.findByCart(cart);
        m.put("items", items.stream().map(i -> {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("itemId", i.getId().toString());
            im.put("productId", i.getProductId());
            im.put("productTitle", i.getProduct() != null ? i.getProduct().getTitle() : "");
            im.put("quantity", i.getQuantity());
            im.put("unitPrice", i.getUnitPrice());
            im.put("totalPrice", i.getTotalPrice());
            return im;
        }).toList());
        m.put("itemCount", items.size());
        return m;
    }
}
