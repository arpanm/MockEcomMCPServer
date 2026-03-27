package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.Address;
import com.mock.ecom.mcpserver.entity.Checkout;
import com.mock.ecom.mcpserver.service.AddressService;
import com.mock.ecom.mcpserver.service.CheckoutService;
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
public class AddressTools {

    private final AddressService addressService;
    private final CheckoutService checkoutService;
    private final ToolResponseHelper helper;

    @Tool(description = "Get list of saved delivery addresses for the authenticated customer. Requires valid sessionId. Returns all addresses with full details including name, phone, address lines, city, state, pincode, and type (HOME/WORK/OTHER). If no addresses exist, mock addresses are auto-generated. Use address ID from response in selectAddress.")
    public String getAddresses(String sessionId) {
        try {
            log.info("[Tool] getAddresses session={}", sessionId);
            List<Address> addresses = addressService.getAddresses(sessionId);
            List<Map<String, Object>> list = addresses.stream().map(this::addressToMap).toList();
            return helper.toJson(Map.of("addresses", list, "count", list.size()));
        } catch (Exception e) {
            log.error("[Tool] getAddresses error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Select a delivery address for an active checkout. Provide checkoutId (from checkout response), addressId (from getAddresses response), and sessionId. Recalculates delivery charges and final grand total based on the selected address pincode. Free delivery for orders above Rs. 499.")
    public String selectAddress(String checkoutId, String addressId, String sessionId) {
        try {
            log.info("[Tool] selectAddress checkoutId={} addressId={} session={}", checkoutId, addressId, sessionId);
            Checkout co = checkoutService.selectAddress(checkoutId, addressId, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("checkoutId", co.getId().toString());
            m.put("status", co.getStatus().name());
            m.put("totalAmount", co.getTotalAmount());
            m.put("discountAmount", co.getDiscountAmount());
            m.put("deliveryCharge", co.getDeliveryCharge());
            m.put("grandTotal", co.getGrandTotal());
            if (co.getAddress() != null) m.put("selectedAddress", addressToMap(co.getAddress()));
            m.put("nextStep", "Call initiatePayment with checkoutId, paymentMethod (UPI/CARD/NETBANKING/COD/WALLET), and sessionId");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] selectAddress error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    private Map<String, Object> addressToMap(Address a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("addressId", a.getId().toString());
        m.put("recipientName", a.getRecipientName());
        m.put("phoneNumber", a.getPhoneNumber());
        m.put("addressLine1", a.getAddressLine1());
        m.put("addressLine2", a.getAddressLine2());
        m.put("city", a.getCity());
        m.put("state", a.getState());
        m.put("pincode", a.getPincode());
        m.put("type", a.getType() != null ? a.getType().name() : null);
        m.put("isDefault", a.isDefault());
        return m;
    }
}
