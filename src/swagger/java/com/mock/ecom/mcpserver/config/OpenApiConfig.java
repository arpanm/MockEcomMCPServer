package com.mock.ecom.mcpserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * Only active when springdoc is on the classpath (i.e. built with -Pswagger).
 * Access the UI at: http://localhost:8080/swagger-ui.html
 * Access the spec at: http://localhost:8080/v3/api-docs
 *
 * NOTE: The 28 MCP tools are NOT traditional REST endpoints; they are invoked
 * via the MCP JSON-RPC protocol over SSE. This spec documents the HTTP
 * transport endpoints and the MCP protocol message format.
 * For Postman-friendly examples of all 28 tools, import docs/postman_collection.json.
 */
@Configuration
@ConditionalOnClass(name = "org.springdoc.core.configuration.SpringDocConfiguration")
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mock Ecommerce MCP Server")
                        .version("1.0.0")
                        .description("""
                                A general-purpose **Mock MCP (Model Context Protocol) Server** \
                                for AI chatbot ecommerce integrations.

                                ## How to use
                                1. Connect an MCP client to `GET /sse` (SSE transport)
                                2. Authenticate: call `serverToServerLogin` to get a `sessionId`
                                3. Use `sessionId` in all cart / order / payment tool calls

                                ## Authentication
                                Platform secret (mock): `mock-platform-secret-key`

                                ## Transport
                                - `GET /sse` — establish SSE connection (MCP protocol)
                                - `POST /mcp/message` — send JSON-RPC tool call

                                ## MCP JSON-RPC format
                                ```json
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "searchProducts",
                                    "arguments": { "query": "laptop", "page": 0, "pageSize": 10 }
                                  }
                                }
                                ```

                                ## Import Postman collection
                                See `docs/postman_collection.json` for all 28 tool examples.
                                """)
                        .contact(new Contact()
                                .name("MockEcomMCPServer")
                                .url("https://github.com/arpanm/MockEcomMCPServer"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local dev"),
                        new Server().url("http://localhost:8081").description("Local dev (alt port)")))
                .tags(List.of(
                        new Tag().name("MCP Transport").description("MCP SSE and message endpoints"),
                        new Tag().name("Health").description("Actuator health and metrics"),
                        new Tag().name("H2 Console").description("H2 in-memory DB console (dev only)")));
    }
}
