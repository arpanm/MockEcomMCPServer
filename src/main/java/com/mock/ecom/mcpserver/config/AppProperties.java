package com.mock.ecom.mcpserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Session session = new Session();
    private Mock mock = new Mock();
    private Pagination pagination = new Pagination();

    @Data
    public static class Session {
        private int ttlHours = 24;
    }

    @Data
    public static class Mock {
        private String platformSecret = "mock-platform-secret-key";
    }

    @Data
    public static class Pagination {
        private int defaultPageSize = 10;
        private int maxPageSize = 50;
    }
}
