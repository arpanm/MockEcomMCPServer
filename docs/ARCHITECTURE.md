# Mock Ecommerce MCP Server — Architecture

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        AI Chatbot Platform                       │
│  ┌──────────────┐    ┌──────────────────────────────────────┐   │
│  │  User Chat   │───▶│         MCP Client (chatbot)         │   │
│  │  Interface   │    │  (Claude, GPT, Gemini, etc.)         │   │
│  └──────────────┘    └───────────────┬──────────────────────┘   │
└──────────────────────────────────────┼──────────────────────────┘
                                       │ MCP Protocol (HTTP/SSE)
                                       │ JSON-RPC 2.0
                                       ▼
┌─────────────────────────────────────────────────────────────────┐
│               Mock Ecommerce MCP Server (Spring Boot)            │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                   MCP Tool Layer (12 files)                │  │
│  │  ProductSearchTools │ ProductDetailTools │ DeliveryTools   │  │
│  │  AuthTools │ CartTools │ AddressTools │ PaymentTools       │  │
│  │  OrderTools │ ShipmentTools │ TicketTools                  │  │
│  │  ReviewTools │ WishlistTools                               │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                               │                                   │
│  ┌───────────────────────────▼───────────────────────────────┐  │
│  │                   Service Layer (12 services)              │  │
│  │  MockDataGeneratorService  │  ProductService               │  │
│  │  AuthService │ AddressService │ CartService                │  │
│  │  CheckoutService │ PaymentService │ OrderService           │  │
│  │  DeliveryService │ ReviewService │ WishlistService         │  │
│  │  TicketService                                             │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                               │                                   │
│  ┌───────────────────────────▼───────────────────────────────┐  │
│  │              Repository Layer (16 repositories)            │  │
│  │  Spring Data JPA — CrudRepository + custom @Query          │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                               │                                   │
│  ┌───────────────────────────▼───────────────────────────────┐  │
│  │             Database (H2 dev / PostgreSQL prod)            │  │
│  │  16 Tables: product, customer, session, cart, cart_item,   │  │
│  │  checkout, orders, order_item, shipment, payment, address, │  │
│  │  review, wishlist_item, ticket, ticket_comment,            │  │
│  │  product_attribute                                         │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 2. MCP Protocol Flow

```
AI Client                    MCP Server
   │                              │
   │── GET /sse ─────────────────▶│  (SSE connection established)
   │◀── SSE: server info ─────────│  (server name, version, tools list)
   │                              │
   │── POST /mcp/messages ───────▶│  {"method": "tools/list"}
   │◀── SSE: tools response ──────│  (28 tool definitions with schemas)
   │                              │
   │── POST /mcp/messages ───────▶│  {"method": "tools/call",
   │                              │   "params": {"name": "searchProducts",
   │                              │              "arguments": {"query": "phone"}}}
   │◀── SSE: tool result ─────────│  (JSON product list)
```

## 3. Mock Data Generation Pipeline

```
  User Query: "buy a gaming laptop"
        │
        ▼
  ┌─────────────────────────────┐
  │  normalizeSearchKey()        │  → "gaming_laptop"
  └─────────────┬───────────────┘
                │
        ┌───────▼────────┐
        │  DB Lookup      │  → findBySearchKey("gaming_laptop")
        └───────┬────────┘
                │
         ┌──────┴──────┐
    Found│             │Not Found
         ▼             ▼
    Return from    detectCategory()  → "ELECTRONICS"
    cache              │
                  generateProduct()
                       │  - pick brand by hash
                       │  - generate title, desc
                       │  - set price in range
                       │  - generate imageUrl
                       │
                  Return product
                       │
                  saveProductsAsync()  ← @Async thread pool
                  (non-blocking)
```

## 4. Session Authentication Flow

```
  Chatbot Server              MCP Server              Database
       │                          │                        │
       │── serverToServerLogin ──▶│                        │
       │   (phone, platform,      │── findByPhone ────────▶│
       │    secret)               │◀── Customer ───────────│
       │                          │── createSession ───────▶│
       │◀── sessionId (UUID) ─────│◀── Session saved ──────│
       │                          │                        │
       │── addToCart ────────────▶│                        │
       │   (productId, qty,       │── validateSession ─────▶│
       │    sessionId)            │◀── Session valid ───────│
       │                          │── cart operations ─────▶│
       │◀── cart response ────────│                        │
```

## 5. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.3.6 |
| MCP Protocol | Spring AI 1.0.0-M6 (`spring-ai-mcp-server-webmvc-spring-boot-starter`) |
| ORM | Spring Data JPA + Hibernate |
| DB (dev) | H2 in-memory |
| DB (prod) | PostgreSQL 16 |
| Async | Spring `@Async` with `ThreadPoolTaskExecutor` (5 core, 20 max, 100 queue) |
| Build | Maven |
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Testing | JUnit 5 + Spring Boot Test |

## 6. Package Structure

```
com.mock.ecom.mcpserver
├── MockEcomMcpServerApplication.java
├── config/
│   ├── McpToolsConfig.java          ← registers all 28 tools via MethodToolCallbackProvider
│   ├── AsyncConfig.java             ← thread pool for async saves
│   └── AppProperties.java           ← type-safe config (session TTL, secret, pagination)
├── entity/                          ← 16 JPA entities
│   ├── Product.java, ProductAttribute.java
│   ├── Customer.java, Session.java
│   ├── Cart.java, CartItem.java
│   ├── Checkout.java
│   ├── Order.java, OrderItem.java, Shipment.java
│   ├── Payment.java, Address.java
│   ├── Review.java, WishlistItem.java
│   ├── Ticket.java, TicketComment.java
├── repository/                      ← 16 JPA repositories
├── service/                         ← 12 service classes
│   └── MockDataGeneratorService.java ← core mock data engine
└── tools/                           ← 12 tool classes (28 @Tool methods)
    └── ToolResponseHelper.java      ← JSON serialization helper
```

## 7. Database Schema (Key Tables)

```sql
product          (id, title, description, category, sub_category, brand, model,
                  image_url, price, mrp, currency, search_key UNIQUE,
                  average_rating, review_count, stock_quantity)
product_attribute(id, product_id FK, name, value)
customer         (id, phone_number UNIQUE, name, email)
session          (id=sessionId, customer_id FK, platform, active, expires_at)
cart             (id, customer_id FK, status, total_amount)
cart_item        (id, cart_id FK, product_id FK, quantity, unit_price, total_price)
checkout         (id, cart_id FK, customer_id FK, address_id FK,
                  status, total_amount, delivery_charge, grand_total)
orders           (id, checkout_id FK, customer_id FK, order_number UNIQUE,
                  status, total_amount, delivery_address)
order_item       (id, order_id FK, product_id FK, shipment_id FK,
                  quantity, unit_price, total_price, status)
shipment         (id, order_id FK, tracking_number, carrier_name, status,
                  delivery_otp, estimated_delivery_date, delivery_pincode)
payment          (id, checkout_id FK, order_id FK, customer_id FK,
                  amount, status, payment_method, transaction_id)
address          (id, customer_id FK, recipient_name, phone_number,
                  address_line1, city, state, pincode, type, is_default)
review           (id, product_id FK, shipment_id FK, customer_id FK,
                  rating, title, description, review_type, is_verified_purchase)
wishlist_item    (id, customer_id FK, product_id FK, added_at)
ticket           (id, customer_id FK, order_id FK, type, priority, status, subject)
ticket_comment   (id, ticket_id FK, author_type, content)
```
