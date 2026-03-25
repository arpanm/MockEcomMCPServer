# Mock Ecommerce MCP Server

A general-purpose **Mock MCP (Model Context Protocol) Server** built with **Java 21 + Spring Boot 3.3.6 + Spring AI 1.0.0-M6**.

One AI chatbot using this server can serve users across **Grocery, Fashion, Electronics, Beauty, and Home** categories. Exposes **28 MCP tools** that any MCP-compatible AI client (Claude, GPT, Gemini, etc.) can discover and invoke.

## Quick Start

```bash
# Run locally (H2 in-memory DB)
mvn spring-boot:run

# MCP SSE endpoint
http://localhost:8080/sse

# H2 console (dev)
http://localhost:8080/h2-console
```

```bash
# Run with Docker + PostgreSQL
mvn clean package -DskipTests
docker-compose up --build
```

## Connect MCP Client

```json
{
  "mcpServers": {
    "mock-ecom": {
      "url": "http://localhost:8080/sse",
      "type": "sse"
    }
  }
}
```

## 28 MCP Tools

| # | Tool | Auth | Description |
|---|---|---|---|
| 1 | `searchProducts` | No | Search by keyword across all categories |
| 2 | `filterProducts` | No | Filter by category / subcategory / brand |
| 3 | `getFilters` | No | Get available filter options for a query |
| 4 | `getSortOptions` | No | Get sort options (price, rating, delivery) |
| 5 | `getProductDetails` | No | Full product details + attributes + images |
| 6 | `getProductReviews` | No | Rating summary + paginated reviews |
| 7 | `getDeliveryTime` | No | Delivery estimate by pincode |
| 8 | `serverToServerLogin` | — | Get sessionId via phone + platform secret |
| 9 | `addToCart` | Yes | Add product to cart |
| 10 | `checkout` | Yes | Initiate checkout from cart |
| 11 | `getAddresses` | Yes | List saved addresses |
| 12 | `selectAddress` | Yes | Select delivery address for checkout |
| 13 | `initiatePayment` | Yes | Pay (UPI/CARD/COD/NETBANKING/WALLET) |
| 14 | `getPaymentStatus` | Yes | Payment status + orderId |
| 15 | `getOrderDetails` | Yes | Order with items and shipments |
| 16 | `getOrders` | Yes | Paginated order history |
| 17 | `getShipments` | Yes | Paginated shipment list |
| 18 | `cancelOrder` | Yes | Cancel order or specific item |
| 19 | `returnOrderItem` | Yes | Initiate item return |
| 20 | `sendDeliveryOtp` | Yes | Send delivery OTP |
| 21 | `getTickets` | Yes | List support tickets |
| 22 | `getTicketDetails` | Yes | Ticket with all comments |
| 23 | `addTicketComment` | Yes | Add comment to ticket |
| 24 | `createTicket` | Yes | Create new support ticket |
| 25 | `submitProductReview` | Yes | Rate and review a product |
| 26 | `submitShipmentReview` | Yes | Rate delivery experience |
| 27 | `addToWishlist` | Yes | Add product to wishlist |
| 28 | `getWishlist` | Yes | View wishlist |

## Authentication

```
# Platform secret (mock)
mock-platform-secret-key

# Get sessionId first, then use it for all authenticated tools
sessionId = serverToServerLogin(phone, platform, "mock-platform-secret-key")
```

## Stack

- **Java 21** + **Spring Boot 3.3.6**
- **Spring AI 1.0.0-M6** — MCP Server via HTTP/SSE
- **Spring Data JPA** — 16 entities, 16 repositories
- **H2** (dev) / **PostgreSQL** (prod)
- **Docker** + **Docker Compose**
- **GitHub Actions** CI

## Documentation

- [Requirements](docs/REQUIREMENTS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Implementation Guide](docs/IMPLEMENTATION.md)
- [SDLC & Sprint Plan](docs/SDLC.md)
- [Task List (TODO)](docs/TODO.md)
