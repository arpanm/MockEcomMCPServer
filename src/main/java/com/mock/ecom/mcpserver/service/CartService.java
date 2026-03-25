package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CheckoutRepository checkoutRepository;
    private final AuthService authService;
    private final ProductService productService;

    @Transactional
    public Cart addToCart(String productId, int quantity, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        Product product = productService.getProductById(UUID.fromString(productId));
        Cart cart = cartRepository.findFirstByCustomerAndStatus(customer, Cart.CartStatus.ACTIVE)
            .orElseGet(() -> cartRepository.save(Cart.builder().customer(customer).status(Cart.CartStatus.ACTIVE).totalAmount(BigDecimal.ZERO).build()));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        if (item != null) {
            item.setQuantity(item.getQuantity() + quantity);
            item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            cartItemRepository.save(item);
        } else {
            cartItemRepository.save(CartItem.builder()
                .cart(cart).product(product)
                .productId(productId)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .build());
        }
        recalculateCart(cart);
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    private void recalculateCart(Cart cart) {
        BigDecimal total = cartItemRepository.findByCart(cart).stream()
            .map(CartItem::getTotalPrice).filter(p -> p != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
        cartRepository.save(cart);
    }

    @Transactional
    public Checkout initiateCheckout(String cartId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        Cart cart = cartRepository.findById(UUID.fromString(cartId))
            .filter(c -> c.getCustomer().getId().equals(customer.getId()))
            .filter(c -> c.getStatus() == Cart.CartStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Active cart not found"));
        cart.setStatus(Cart.CartStatus.CHECKED_OUT);
        cartRepository.save(cart);
        Checkout checkout = Checkout.builder()
            .cart(cart).customer(customer)
            .status(Checkout.CheckoutStatus.PENDING)
            .totalAmount(cart.getTotalAmount())
            .discountAmount(BigDecimal.ZERO)
            .deliveryCharge(BigDecimal.ZERO)
            .grandTotal(cart.getTotalAmount())
            .build();
        return checkoutRepository.save(checkout);
    }
}
