package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.repository.CheckoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

    private final CheckoutRepository checkoutRepository;
    private final AddressService addressService;
    private final AuthService authService;

    public Checkout getCheckout(UUID checkoutId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return checkoutRepository.findById(checkoutId)
            .filter(c -> c.getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Checkout not found"));
    }

    @Transactional
    public Checkout selectAddress(String checkoutId, String addressId, String sessionId) {
        Checkout checkout = getCheckout(UUID.fromString(checkoutId), sessionId);
        Address address = addressService.getAddress(UUID.fromString(addressId), sessionId);
        checkout.setAddress(address);
        checkout.setStatus(Checkout.CheckoutStatus.ADDRESS_SELECTED);
        BigDecimal deliveryCharge = calculateDelivery(address.getPincode(), checkout.getTotalAmount());
        checkout.setDeliveryCharge(deliveryCharge);
        checkout.setGrandTotal(checkout.getTotalAmount().subtract(checkout.getDiscountAmount()).add(deliveryCharge));
        return checkoutRepository.save(checkout);
    }

    private BigDecimal calculateDelivery(String pincode, BigDecimal orderTotal) {
        if (orderTotal.compareTo(BigDecimal.valueOf(499)) >= 0) return BigDecimal.ZERO;
        String prefix = pincode != null && pincode.length() >= 3 ? pincode.substring(0, 3) : "999";
        Set<String> metro = Set.of("110", "400", "560", "600", "700", "500");
        return metro.contains(prefix) ? BigDecimal.valueOf(29) : BigDecimal.valueOf(49);
    }
}
