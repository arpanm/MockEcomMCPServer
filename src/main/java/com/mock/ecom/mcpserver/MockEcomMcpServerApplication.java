package com.mock.ecom.mcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockEcomMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockEcomMcpServerApplication.class, args);
    }
}
