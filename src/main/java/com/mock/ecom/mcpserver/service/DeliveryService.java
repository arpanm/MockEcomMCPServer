package com.mock.ecom.mcpserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryService {

    private static final Set<String> METRO_PREFIXES = Set.of("110","111","400","401","560","561","600","601","700","500","501");
    private static final Set<String> TIER2_PREFIXES = Set.of("302","411","380","462","226","641","682","800","440","530");
    private static final String[] CARRIERS = {"Delhivery","BlueDart","DTDC","Ekart","Xpressbees","Shadowfax"};

    public Map<String, Object> getDeliveryTime(String productId, String pincode) {
        String prefix = pincode != null && pincode.length() >= 3 ? pincode.substring(0, 3) : "999";
        int standardDays = METRO_PREFIXES.contains(prefix) ? 2 : TIER2_PREFIXES.contains(prefix) ? 4 : 6;
        int expressDays  = Math.max(1, standardDays - 1);
        String carrier = CARRIERS[Math.abs(pincode != null ? pincode.hashCode() : 0) % CARRIERS.length];
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", productId);
        result.put("pincode", pincode);
        result.put("freeDeliveryThreshold", 499);
        result.put("deliveryOptions", List.of(
            Map.of("type","STANDARD","estimatedDays",standardDays,"estimatedDate",LocalDate.now().plusDays(standardDays).toString(),"charge",0,"carrier",carrier),
            Map.of("type","EXPRESS","estimatedDays",expressDays,"estimatedDate",LocalDate.now().plusDays(expressDays).toString(),"charge",expressDays<=1?99:49,"carrier","BlueDart")
        ));
        return result;
    }
}
