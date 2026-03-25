package com.mock.ecom.mcpserver.config;

import com.mock.ecom.mcpserver.tools.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider mcpTools(
            ProductSearchTools productSearchTools,
            ProductDetailTools productDetailTools,
            DeliveryTools deliveryTools,
            AuthTools authTools,
            CartTools cartTools,
            AddressTools addressTools,
            PaymentTools paymentTools,
            OrderTools orderTools,
            ShipmentTools shipmentTools,
            TicketTools ticketTools,
            ReviewTools reviewTools,
            WishlistTools wishlistTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        productSearchTools,
                        productDetailTools,
                        deliveryTools,
                        authTools,
                        cartTools,
                        addressTools,
                        paymentTools,
                        orderTools,
                        shipmentTools,
                        ticketTools,
                        reviewTools,
                        wishlistTools)
                .build();
    }
}
