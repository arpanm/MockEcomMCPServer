# Mock Ecommerce MCP Server

A general-purpose **Mock MCP (Model Context Protocol) Server** built with **Java 21 + Spring Boot 3.3.6 + Spring AI 1.0.0-M6**.

One AI chatbot using this server can serve users across **Grocery, Fashion, Electronics, Beauty, and Home** categories, and **food delivery** (Swiggy-style restaurants). Exposes **40+ MCP tools** that any MCP-compatible AI client (Claude, GPT, Gemini, etc.) can discover and invoke.

---

## Table of Contents

1. [Requirements](#requirements)
2. [Architecture](#architecture)
3. [Code Structure](#code-structure)
4. [Local Runtime — Quick Start](#local-runtime--quick-start)
5. [Web UI — Data Explorer](#web-ui--data-explorer)
6. [Local Endpoints](#local-endpoints)
7. [The 28 E-commerce MCP Tools](#the-28-mcp-tools)
8. [Restaurant & Swiggy Scraper Tools](#restaurant--swiggy-scraper-tools)
9. [Seed Data Pipeline](#seed-data-pipeline)
10. [Test Cases](#test-cases)
11. [Running Tests Locally](#running-tests-locally)
12. [Production Deployment](#production-deployment)
13. [Connecting MCP Clients](#connecting-mcp-clients)
14. [Status & TODOs](#status--todos)

---

## Requirements

### Functional Requirements

- Expose **28 MCP tools** over HTTP/SSE for AI chatbot integration
- Support **5 product categories**: Grocery, Fashion, Electronics, Beauty, Home
- Full ecommerce flow: search → cart → checkout → payment → order → shipment → review
- Session-based authentication via phone number + platform secret
- Mock data generation — no real database seed needed; products are generated on-demand
- Support tickets and wishlist management

### Non-Functional Requirements

- **Java 21** minimum
- In-memory H2 database for local/dev; PostgreSQL for production
- All tools respond with structured JSON strings
- Session TTL: 24 hours
- Pagination: default 10 items, max 50
- Async product generation to avoid blocking search responses

### Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 21+ |
| Maven | 3.8+ |
| Docker + Docker Compose | Any recent version (for prod mode) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        MCP Client                           │
│          (Claude Desktop / VS Code / Custom App)            │
└─────────────────────┬───────────────────────────────────────┘
                      │  HTTP/SSE  (MCP Protocol)
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot MCP Server  :8080                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  MCP Tool Layer  (12 Tool Classes, 28 Tools)         │   │
│  │  ProductSearch · ProductDetail · Delivery · Auth     │   │
│  │  Cart · Address · Payment · Order · Shipment         │   │
│  │  Ticket · Review · Wishlist                          │   │
│  └──────────────────────┬───────────────────────────────┘   │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │  Service Layer  (12 Services)                        │   │
│  │  Auth · Product · Cart · Checkout · Order · Payment  │   │
│  │  Address · Delivery · Review · Ticket · Wishlist     │   │
│  │  MockDataGenerator                                   │   │
│  └──────────────────────┬───────────────────────────────┘   │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │  Repository Layer  (16 Spring Data JPA Repos)        │   │
│  └──────────────────────┬───────────────────────────────┘   │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │  Database: H2 (dev/test)  │  PostgreSQL (prod)       │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow — End-to-End Shopping Journey

```
Login (phone) → sessionId
  → searchProducts / filterProducts
    → addToCart (sessionId)
      → checkout (cartId, sessionId)
        → selectAddress (checkoutId, addressId, sessionId)
          → initiatePayment (checkoutId, method, sessionId)
            → orderId created automatically
              → getOrderDetails / getShipments
                → submitProductReview / createTicket
```

### Key Design Decisions

- **Mock data on demand** — products are generated deterministically from a search key + category; no manual seeding needed
- **SSE transport** — tools are exposed over Server-Sent Events for real-time streaming support
- **H2 in-memory** for dev means zero setup; schema is created on startup and dropped on exit
- **Async generation** — when a search returns no results, 8 products are generated asynchronously in the background

---

## Code Structure

```
src/
├── main/java/com/mock/ecom/mcpserver/
│   ├── MockEcomMcpServerApplication.java   # Entry point (@EnableAsync)
│   ├── config/
│   │   ├── AppProperties.java              # App config (session TTL, secret, pagination)
│   │   ├── AsyncConfig.java                # Thread pool (5 core, 20 max, 100 queue)
│   │   └── McpToolsConfig.java             # Registers all 12 tool classes with MCP
│   ├── entity/                             # 16 JPA entities
│   │   ├── Customer, Session, Address
│   │   ├── Product, ProductAttribute
│   │   ├── Cart, CartItem, Checkout
│   │   ├── Order, OrderItem, Payment
│   │   ├── Shipment, Review
│   │   ├── Ticket, TicketComment, WishlistItem
│   ├── repository/                         # 16 Spring Data JPA repositories
│   ├── service/                            # 12 business services
│   │   ├── AuthService.java                # Session create/validate, customer lookup
│   │   ├── ProductService.java             # Search, filter, reviews, lazy generation
│   │   ├── MockDataGeneratorService.java   # Deterministic mock data for 5 categories
│   │   ├── CartService.java                # Add to cart, recalculate totals, checkout
│   │   ├── CheckoutService.java            # Checkout lifecycle
│   │   ├── OrderService.java               # Order creation and status transitions
│   │   ├── PaymentService.java             # Payment initiation and status
│   │   ├── AddressService.java             # Address CRUD
│   │   ├── DeliveryService.java            # Delivery estimate by pincode
│   │   ├── ReviewService.java              # Product and shipment reviews
│   │   ├── TicketService.java              # Support tickets and comments
│   │   └── WishlistService.java            # Wishlist add/view
│   └── tools/                              # 12 MCP tool classes
│       ├── ProductSearchTools.java         # Tools 1-4
│       ├── ProductDetailTools.java         # Tools 5-6
│       ├── DeliveryTools.java              # Tool 7
│       ├── AuthTools.java                  # Tool 8
│       ├── CartTools.java                  # Tools 9-10
│       ├── AddressTools.java               # Tools 11-12
│       ├── PaymentTools.java               # Tools 13-14
│       ├── OrderTools.java                 # Tools 15-19
│       ├── ShipmentTools.java              # Tool 20
│       ├── TicketTools.java                # Tools 21-24
│       ├── ReviewTools.java                # Tools 25-26
│       ├── WishlistTools.java              # Tools 27-28
│       └── ToolResponseHelper.java         # JSON serialization utility
└── main/resources/
    ├── application.yml                     # Dev profile (H2, port 8080)
    ├── application-test.yml                # Test profile (H2, separate DB)
    └── application-prod.yml                # Prod profile (PostgreSQL, env vars)

src/test/java/com/mock/ecom/mcpserver/
├── MockEcomMcpServerApplicationTests.java
└── integration/
    ├── EndToEndShoppingJourneyTest.java    # Full 28-tool flow
    ├── AuthAndCartFlowTest.java            # Tools 8-14
    ├── ProductToolsIntegrationTest.java    # Tools 1-7
    ├── OrderManagementTest.java            # Tools 15-20
    ├── TicketAndReviewTest.java            # Tools 21-26
    ├── WishlistTest.java                   # Tools 27-28
    └── MockDataGeneratorTest.java          # Data generation logic
```

### Entity Relationships

```
Customer ──< Session
Customer ──< Address
Customer ──< Cart ──< CartItem >── Product
Customer ──< WishlistItem >── Product
Cart ──── Checkout ──── Order ──< OrderItem >── Product
Checkout ──< Payment
Order ──< Shipment ──< OrderItem
Order ──< Ticket ──< TicketComment
Shipment ──< Review (DELIVERY type)
Product ──< Review (PRODUCT type)
Product ──< ProductAttribute
```

### Enums Reference

| Entity | Status Values |
|--------|--------------|
| Cart | `ACTIVE`, `CHECKED_OUT`, `ABANDONED` |
| Checkout | `PENDING`, `ADDRESS_SELECTED`, `PAYMENT_INITIATED`, `COMPLETED`, `CANCELLED` |
| Order | `PLACED`, `PROCESSING`, `PARTIALLY_SHIPPED`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `PARTIALLY_RETURNED`, `RETURNED` |
| OrderItem | `ORDERED`, `PROCESSING`, `PACKED`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`, `RETURN_INITIATED`, `RETURNED`, `REFUNDED` |
| Payment | `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `REFUND_INITIATED`, `REFUNDED`, `PARTIALLY_REFUNDED` |
| Shipment | `PENDING`, `PROCESSING`, `PACKED`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `RETURNED`, `CANCELLED` |
| Ticket | `OPEN`, `IN_PROGRESS`, `AWAITING_CUSTOMER`, `RESOLVED`, `CLOSED` |
| TicketType | `DELIVERY_ISSUE`, `RETURN_REQUEST`, `PAYMENT_ISSUE`, `PRODUCT_QUALITY`, `CANCELLATION`, `OTHER` |
| AddressType | `HOME`, `WORK`, `OTHER` |
| ReviewType | `PRODUCT`, `DELIVERY` |
| PaymentMethod | `UPI`, `CARD`, `COD`, `NETBANKING`, `WALLET` |

---

## Local Runtime — Quick Start

### Option 1: One-Command Script (Recommended)

The `run-local.sh` script automatically finds a free port, updates the config, builds, and starts the server.

```bash
chmod +x run-local.sh
./run-local.sh
```

What it does:
- Checks if port 8080 is available; if not, finds the next free port (8081, 8082, …)
- Updates `server.port` in `application.yml` temporarily
- Builds with Maven (skipping tests for speed)
- Starts the Spring Boot application
- Prints all endpoint URLs with the actual port

### Option 2: Manual Steps

```bash
# 1. Build
mvn clean package -DskipTests

# 2. Run (H2 in-memory, dev profile)
mvn spring-boot:run

# Or run the JAR directly
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar
```

### Option 3: Custom Port

```bash
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar --server.port=9090
```

### Option 4: Docker + PostgreSQL

```bash
# Build and start all services
mvn clean package -DskipTests
docker-compose up --build

# Stop
docker-compose down

# Stop and remove data volume
docker-compose down -v
```

### Environment Variables (Docker / Production)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Set to `prod` for PostgreSQL |
| `DB_URL` | `jdbc:postgresql://localhost:5432/mockecomdb` | JDBC URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `SERVER_PORT` | `8080` | HTTP listen port |

---

## Web UI — Data Explorer

A React single-page application is bundled into the JAR and served automatically at the root URL. It lets you visually browse all scraped and generated data with live, hierarchical filters.

### Accessing the UI

Start the server (see [Local Runtime](#local-runtime--quick-start)), then open:

```
http://localhost:8080
```

No additional setup is required — the pre-built UI is already included in `src/main/resources/static/` and served by Spring Boot.

### Pages

| Page | URL | Description |
|------|-----|-------------|
| **Home / Dashboard** | `http://localhost:8080/#/` | Live stats (cities, restaurants, menu items), scraper progress bars, quick navigation |
| **Restaurant Explorer** | `http://localhost:8080/#/restaurants` | Browse all restaurants with hierarchical filters and menu viewer |
| **Product Explorer** | `http://localhost:8080/#/products` | Browse mock-generated products by category, brand, sub-category |

### Restaurant Explorer features

1. **City filter** (left sidebar) — lists every city that has restaurants, with count badges. Click to narrow results.
2. **Cuisine filter** — automatically populated from the selected city's restaurant data. Click one or more cuisines to filter.
3. **Pure Veg toggle** — shows only vegetarian restaurants.
4. **Search bar** — debounced search by restaurant name.
5. **Menu Item Search mode** — switch to search for a specific dish (e.g. "biryani", "dosa") across all restaurants in the selected city.
6. **Restaurant cards** — rating badge, cuisine tags, cost for two, delivery time, discount offers, closed/open indicator.
7. **Restaurant detail panel** — slide-in from the right on card click:
   - Full info: rating, ratings count, delivery time, cost, city, discount
   - Full menu grouped by category with collapsible sections
   - Category tabs to jump to Starters / Biryani / Desserts, etc.
   - Per-item: veg/non-veg indicator, ⭐ Bestseller badge, price, out-of-stock label

### Product Explorer features

1. **Category filter** (left sidebar) — Electronics, Fashion, Grocery, Beauty, Home.
2. **Sub-category filter** — dynamically loaded from the API for the current search/category.
3. **Brand filter** — dynamically loaded brands with product counts.
4. **Search bar** — debounced full-text search across product names and descriptions.
5. **Product cards** — brand, title, star rating, price with original MRP strikethrough, discount % badge.
6. **Product detail panel** — slide-in from the right on card click:
   - Full description, category breadcrumb, pricing with savings
   - Specifications table (all product attributes)
   - Customer reviews (expandable, paginated)

### Building / rebuilding the UI

The built output (`src/main/resources/static/`) is committed so the app works out of the box. If you modify any UI source in `ui/src/`, rebuild before running:

**Prerequisites:** Node.js 18+ and npm.

```bash
# Install dependencies (first time only)
cd ui
npm install

# Build — outputs to src/main/resources/static/
npm run build

# Then rebuild the JAR so Spring Boot picks up the new assets
cd ..
mvn clean package -DskipTests
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar
```

The UI is then accessible at `http://localhost:8080`.

### UI development with hot reload

To iterate on the UI without rebuilding the JAR every time, run the Vite dev server alongside the Spring Boot server:

```bash
# Terminal 1 — start the backend
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar

# Terminal 2 — start the Vite dev server
cd ui
npm run dev
```

Open `http://localhost:5173` in your browser. The Vite dev server proxies all `/api` requests to `http://localhost:8080`, so the UI talks to the live backend. Changes to `ui/src/` are reflected instantly with hot module replacement.

After finishing development, rebuild and commit:

```bash
cd ui && npm run build
cd ..
git add src/main/resources/static/
git commit -m "chore: update UI build"
```

### UI tech stack

| Tool | Version | Purpose |
|------|---------|---------|
| React | 18 | UI framework |
| TypeScript | 5 | Type safety |
| Vite | 5 | Build tool + dev server |
| Tailwind CSS | 3 | Utility-first styling |
| TanStack Query | 5 | Data fetching, caching, auto-refresh |
| Axios | 1 | HTTP client |
| React Router | 6 | Client-side routing (HashRouter) |
| Lucide React | — | Icons |

### REST API endpoints (used by the UI)

All endpoints under `/api/v1/` are also available for direct use (curl, Postman, other clients).

| Method | Endpoint | Parameters | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/stats` | — | Aggregate counts (cities, restaurants, menu items) |
| GET | `/api/v1/cities` | — | All cities with restaurant counts and scrape status |
| GET | `/api/v1/cuisines` | `cityName` (optional) | Distinct cuisine list for a city (or all cities) |
| GET | `/api/v1/restaurants` | `cityName`, `cuisine`, `name`, `isPureVeg`, `page`, `size` | Paginated restaurant search |
| GET | `/api/v1/restaurants/{swiggyId}` | — | Single restaurant by Swiggy ID |
| GET | `/api/v1/restaurants/{swiggyId}/menu` | — | Full menu grouped by category |
| GET | `/api/v1/menu-items` | `q` (required), `cityName`, `page`, `size` | Search dishes by name across all restaurants |
| GET | `/api/v1/products` | `q`, `category`, `subCategory`, `brand`, `page`, `size` | Paginated product search |
| GET | `/api/v1/products/filters` | `q` | Available categories, sub-categories, brands for a query |
| GET | `/api/v1/products/{productId}` | — | Full product detail with attributes |
| GET | `/api/v1/products/{productId}/reviews` | `page`, `size` | Paginated product reviews |
| GET | `/api/v1/scraper/status` | — | Scraper running state + counts |

Example curl calls:

```bash
# All cities with restaurants
curl http://localhost:8080/api/v1/cities

# Restaurants in Bangalore
curl "http://localhost:8080/api/v1/restaurants?cityName=Bangalore"

# Veg restaurants in Mumbai
curl "http://localhost:8080/api/v1/restaurants?cityName=Mumbai&isPureVeg=true"

# Cuisines available in Hyderabad
curl "http://localhost:8080/api/v1/cuisines?cityName=Hyderabad"

# Full menu for Paradise Biryani
curl "http://localhost:8080/api/v1/restaurants/400001/menu"

# Search for "biryani" across all restaurants
curl "http://localhost:8080/api/v1/menu-items?q=biryani"

# Search for "biryani" in Bangalore only
curl "http://localhost:8080/api/v1/menu-items?q=biryani&cityName=Bangalore"

# Electronics products
curl "http://localhost:8080/api/v1/products?category=Electronics&size=5"

# Laptop products
curl "http://localhost:8080/api/v1/products?q=laptop"
```

---

## Local Endpoints

All endpoints are on `http://localhost:<PORT>` (default `8080`).

| Endpoint | Description |
|----------|-------------|
| `GET /` | **Web UI** — React data explorer (restaurants, products, scraper dashboard) |
| `GET /api/v1/**` | **REST API** — used by the UI; also available for direct curl/Postman use |
| `GET /sse` | **MCP SSE endpoint** — connect your MCP client here |
| `POST /mcp/message` | MCP message endpoint (used internally by SSE transport) |
| `GET /actuator/health` | Health check — returns `{"status":"UP"}` |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | Metrics list |
| `GET /actuator/metrics/{name}` | Specific metric value |
| `GET /h2-console` | H2 database browser **(dev only, username: sa, no password)** |
| `GET /swagger-ui.html` | Swagger UI — interactive API docs **(requires `-Pswagger`)** |
| `GET /v3/api-docs` | OpenAPI 3.0 JSON spec **(requires `-Pswagger`)** |

### API Documentation

| Resource | Location | Notes |
|----------|----------|-------|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | Live interactive docs, build with `mvn package -Pswagger` |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` | Machine-readable spec, requires `-Pswagger` |
| **OpenAPI YAML** | [`docs/openapi.yaml`](docs/openapi.yaml) | Static spec, always available (no build flag needed) |
| **Postman Collection** | [`docs/postman_collection.json`](docs/postman_collection.json) | Import into Postman; all 28 tools pre-configured |

#### Enable Swagger UI

Build with the `swagger` Maven profile to activate the live Swagger UI:

```bash
# Build with Swagger
mvn clean package -Pswagger -DskipTests

# Run
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar

# Open in browser
open http://localhost:8080/swagger-ui.html
```

#### Import Postman Collection

1. Open Postman → **Import** → select `docs/postman_collection.json`
2. Set the `baseUrl` collection variable (default: `http://localhost:8080`)
3. Run **Authentication → serverToServerLogin** first — the `sessionId` is auto-saved
4. Use the saved `sessionId` in all subsequent requests

### H2 Console Connection

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/mockecomdb
Username: sa
Password: (leave blank)
```

### Health Check Example

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

---

## The 28 MCP Tools

### Product Discovery — No Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 1 | `searchProducts` | `query`, `page`, `pageSize` | Full-text search across all categories. Generates mock products if none found. |
| 2 | `filterProducts` | `query`, `category`, `subCategory`, `brand`, `page`, `pageSize` | Filter products by any combination of category / subcategory / brand. |
| 3 | `getFilters` | `query` | Returns available filter facets (categories, subcategories, brands) with counts. |
| 4 | `getSortOptions` | `query` | Returns 6 sort options: `price_asc`, `price_desc`, `rating_desc`, `newest`, `popularity`, `delivery_asc`. |
| 5 | `getProductDetails` | `productId` | Full product — title, description, brand, model, price, MRP, rating, attributes, images. |
| 6 | `getProductReviews` | `productId`, `page`, `pageSize` | Rating summary + paginated reviews with rating distribution. |
| 7 | `getDeliveryTime` | `productId`, `pincode` | Delivery estimate with date range for a given pincode. |

### Authentication — Server-to-Server

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 8 | `serverToServerLogin` | `phone`, `platform`, `secret` | Get `sessionId`. Secret = `mock-platform-secret-key`. Creates customer if new. |

### Cart — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 9 | `addToCart` | `productId`, `quantity`, `sessionId` | Adds product to active cart. Creates cart if none exists. Recalculates total. |
| 10 | `checkout` | `cartId`, `sessionId` | Converts cart to checkout. Returns `checkoutId` and grand total. |

### Address — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 11 | `getAddresses` | `sessionId` | Lists all saved addresses for the customer. |
| 12 | `selectAddress` | `checkoutId`, `addressId`, `sessionId` | Attaches a delivery address to the checkout. |

### Payment — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 13 | `initiatePayment` | `checkoutId`, `paymentMethod`, `sessionId` | Initiates payment. Methods: `UPI`, `CARD`, `COD`, `NETBANKING`, `WALLET`. Creates the Order. |
| 14 | `getPaymentStatus` | `paymentId`, `sessionId` | Returns payment status and associated `orderId`. |

### Orders — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 15 | `getOrderDetails` | `orderId`, `sessionId` | Full order with all items, statuses, and shipment info. |
| 16 | `getOrders` | `sessionId`, `page`, `pageSize` | Paginated order history. |
| 17 | `getShipments` | `sessionId`, `page`, `pageSize` | Paginated shipment list with tracking numbers. |
| 18 | `cancelOrder` | `orderId`, `itemId`, `sessionId` | Cancel entire order or a specific item. |
| 19 | `returnOrderItem` | `orderItemId`, `reason`, `sessionId` | Initiate return for a specific item. |

### Shipment — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 20 | `sendDeliveryOtp` | `shipmentId`, `sessionId` | Generates and sends delivery OTP for the shipment. |

### Support Tickets — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 21 | `getTickets` | `sessionId`, `page`, `pageSize` | Paginated list of customer support tickets. |
| 22 | `getTicketDetails` | `ticketId`, `sessionId` | Ticket detail with full comment thread. |
| 23 | `addTicketComment` | `ticketId`, `comment`, `sessionId` | Appends a customer comment to a ticket. |
| 24 | `createTicket` | `subject`, `description`, `type`, `orderId`, `orderItemId`, `sessionId` | Creates a new ticket. Types: `DELIVERY_ISSUE`, `RETURN_REQUEST`, `PAYMENT_ISSUE`, `PRODUCT_QUALITY`, `CANCELLATION`, `OTHER`. |

### Reviews — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 25 | `submitProductReview` | `productId`, `rating`, `title`, `description`, `sessionId` | Rate and review a product (1–5 stars). Marks as verified purchase if ordered before. |
| 26 | `submitShipmentReview` | `shipmentId`, `rating`, `title`, `description`, `sessionId` | Rate the delivery experience. |

### Wishlist — Auth Required

| # | Tool | Parameters | Description |
|---|------|-----------|-------------|
| 27 | `addToWishlist` | `productId`, `sessionId` | Adds product to wishlist. Duplicate-safe. |
| 28 | `getWishlist` | `sessionId`, `page`, `pageSize` | Returns paginated wishlist with product details. |

### Authentication Flow

```
# Step 1 — Get a session
sessionId = serverToServerLogin(
  phone    = "9999999999",
  platform = "WEB",
  secret   = "mock-platform-secret-key"
)

# Step 2 — Use sessionId in all authenticated tools
addToCart(productId=<id>, quantity=1, sessionId=<sessionId>)
```

---

## Restaurant & Swiggy Scraper Tools

The server includes a full Swiggy data scraper and restaurant query layer, adding **12 new MCP tools** for food delivery use cases.

### How it works on startup

| Order | Component | Action |
|-------|-----------|--------|
| 1 | `CityDataInitializer` | Seeds 50 major Indian cities with lat/lng (idempotent) |
| 2 | `SeedDataLoader` | If `classpath:db/seed/seed-data.json` exists and DB is empty, loads all scraped restaurants and menus |
| 3 | `SampleRestaurantDataSeeder` | If DB is still empty (no seed file), loads 10 hardcoded demo restaurants across Bangalore, Mumbai, Delhi, Hyderabad, Chennai |
| 4 | `ScraperAutoStartRunner` | If `app.scraper.auto-start=true`, kicks off live scraping asynchronously |

### Restaurant query tools (6 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `listRestaurantCities` | — | All 50 Indian cities with restaurant counts and scrape status |
| `searchRestaurants` | `cityName`, `cuisine`, `name`, `isPureVeg`, `page`, `pageSize` | Search by any combination; results sorted by rating |
| `getRestaurantBySwiggyId` | `swiggyId` | Full restaurant details |
| `getRestaurantMenu` | `swiggyId` | Full menu grouped by category with item prices (in INR) |
| `searchMenuItems` | `query`, `cityName`, `page`, `pageSize` | Find dishes by name across all restaurants in a city |
| `filterRestaurantMenuItems` | `swiggyId`, `isVeg`, `inStock` | Filter a restaurant's menu by veg/non-veg, availability |

### Scraper control tools (6 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `getScraperStatus` | — | City/restaurant/menu counts; whether scraping is running |
| `startRestaurantScraping` | — | Start async scraping for all un-scraped cities (skips already-done) |
| `scrapeRestaurantsForCity` | `cityName` | Scrape a single city synchronously; returns count of new restaurants |
| `startMenuScraping` | — | Start async menu scraping for all restaurants without menus |
| `scrapeRestaurantMenu` | `swiggyId` | Scrape one restaurant's menu synchronously |
| `exportSeedData` | — | Export all data to `./seed-export/seed-data.json` for committing |

### Scraping live data from Swiggy

> **Requires:** Network access to `www.swiggy.com`. The server must be running on a machine that can reach Swiggy's API.

**Step 1 – Scrape all restaurants:**
```json
{ "name": "startRestaurantScraping", "arguments": {} }
```
Monitor with `getScraperStatus`. Each city takes ~30–120 seconds (1.5 s rate limit). All 50 cities take ~1–2 hours.

**Step 2 – Scrape menus:**
```json
{ "name": "startMenuScraping", "arguments": {} }
```
May take several hours for thousands of restaurants. Both scrapers are **incremental** – safe to interrupt and resume anytime.

**Step 3 – Check progress:**
```json
{ "name": "getScraperStatus", "arguments": {} }
```
Returns `totalCities`, `scrapedCities`, `totalRestaurants`, `restaurantsWithMenu`, and running flags.

---

## Seed Data Pipeline

The seed data pipeline lets you commit scraped data to git so every new deployment starts fully loaded — no re-scraping required.

### Export scraped data

After scraping, call the `exportSeedData` MCP tool:
```json
{ "name": "exportSeedData", "arguments": {} }
```

This writes `./seed-export/seed-data.json` — a portable JSON file containing all restaurants and their full menus.

### Commit the seed file

```bash
cp seed-export/seed-data.json src/main/resources/db/seed/seed-data.json
git add src/main/resources/db/seed/seed-data.json
git commit -m "chore: add scraped Swiggy restaurant seed data (N restaurants)"
git push
```

### Rebuild and deploy

```bash
mvn clean package -DskipTests
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar
```

On first startup with a fresh database, `SeedDataLoader` detects the seed file and imports all data automatically. `SampleRestaurantDataSeeder` is skipped when data is already present.

### Incremental updates after re-scraping

When you re-run the scraper to pick up new restaurants:
- **Cities:** `scrapeRestaurantsForCity` upserts by Swiggy ID — existing restaurants are updated, new ones are inserted
- **Menus:** `startMenuScraping` skips restaurants where `menuScraped = true`; use `scrapeRestaurantMenu` to force-refresh a single restaurant
- After re-scraping, call `exportSeedData` again, copy the new file, and commit

### Data persistence (development)

The development H2 database persists in `./data/mockecomdb.*` (file-based H2). Data survives server restarts. The `./data/` directory is `.gitignore`d — only the exported `seed-data.json` in `src/main/resources/db/seed/` is committed.

---

## Test Cases

### Test Suites (7 files, 83 tests)

| File | Tools Covered | Test Count | Key Scenarios |
|------|--------------|-----------|---------------|
| `MockEcomMcpServerApplicationTests` | — | 1 | Spring context loads successfully |
| `ProductToolsIntegrationTest` | 1–7 | ~20 | Multi-category search, pagination, filters, delivery times |
| `AuthAndCartFlowTest` | 8–14 | ~9 | Login valid/invalid, cart lifecycle, checkout, payment |
| `OrderManagementTest` | 15–20 | ~15 | Order retrieval, cancellation, returns, OTP, shipment tracking |
| `TicketAndReviewTest` | 21–26 | ~18 | Ticket creation (all types), comments, product and delivery reviews |
| `WishlistTest` | 27–28 | ~10 | Add, view, pagination, duplicate prevention |
| `EndToEndShoppingJourneyTest` | 1–28 | ~10 | Full shopping journeys across all 5 categories |
| `MockDataGeneratorTest` | — | ~10 | Category detection, product generation, attribute generation |

### End-to-End Flow (EndToEndShoppingJourneyTest)

```
1.  serverToServerLogin        → sessionId
2.  searchProducts             → productId
3.  filterProducts             → filtered list
4.  getFilters                 → filter facets
5.  getSortOptions             → sort options
6.  getProductDetails          → product info
7.  getProductReviews          → reviews
8.  getDeliveryTime            → estimate
9.  addToWishlist              → wishlistItem
10. getWishlist                → wishlist
11. addToCart (x2 products)    → cartId
12. checkout                   → checkoutId
13. getAddresses               → addressId
14. selectAddress              → confirmed
15. initiatePayment (UPI)      → paymentId, orderId
16. getPaymentStatus           → SUCCESS
17. getOrderDetails            → order + items
18. getOrders                  → history
19. getShipments               → shipmentId
20. sendDeliveryOtp            → OTP sent
21. createTicket               → ticketId
22. getTickets                 → list
23. getTicketDetails           → ticket + comments
24. addTicketComment           → comment added
25. submitProductReview        → review saved
26. submitShipmentReview       → review saved
```

---

## Running Tests Locally

```bash
# Run all tests (uses test profile with separate H2 DB)
mvn test

# Run a specific test class
mvn test -Dtest=EndToEndShoppingJourneyTest

# Run a specific test method
mvn test -Dtest=ProductToolsIntegrationTest#testSearchProducts

# Run with verbose output
mvn test -Dsurefire.useFile=false

# Run tests + generate coverage report (if jacoco configured)
mvn verify

# Skip tests during build
mvn clean package -DskipTests
```

### Test Profile

Tests run with `application-test.yml`:
- Separate H2 in-memory DB: `jdbc:h2:mem:testdb`
- Schema created fresh for each test run (`ddl-auto: create-drop`)
- `NON_KEYWORDS=VALUE` added to avoid H2 SQL keyword conflicts
- SQL logging disabled for cleaner output

### CI (GitHub Actions)

Tests run automatically on every push/PR via `.github/workflows/build.yml`:
- Java 21 (temurin), Maven cache
- `mvn clean verify -Dspring.profiles.active=test`
- JAR artifact uploaded on success

---

## Production Deployment

### Docker Compose (Recommended)

```bash
# Build the JAR
mvn clean package -DskipTests

# Start app + PostgreSQL
docker-compose up --build -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

Services started:
- **app** — Spring Boot on port `8080`, profile `prod`
- **postgres** — PostgreSQL 16 on port `5432`, DB `mockecomdb`

PostgreSQL data persisted in Docker volume `pgdata`.

### Custom Environment Variables

```bash
docker-compose up \
  -e DB_PASSWORD=supersecret \
  -e SERVER_PORT=9000 \
  --build
```

Or create a `.env` file alongside `docker-compose.yml`:

```env
DB_URL=jdbc:postgresql://myhost:5432/mydb
DB_USERNAME=myuser
DB_PASSWORD=mysecret
SPRING_PROFILES_ACTIVE=prod
```

### Manual JAR Deployment

```bash
# Build
mvn clean package -DskipTests

# Run with prod profile
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080 \
  --DB_URL=jdbc:postgresql://localhost:5432/mockecomdb \
  --DB_USERNAME=postgres \
  --DB_PASSWORD=postgres
```

### Kubernetes / Cloud

The server is stateless (all state in the DB), so it can be scaled horizontally. Key considerations:

- Set `spring.jpa.hibernate.ddl-auto=update` (already set in `application-prod.yml`)
- Use a managed PostgreSQL service (RDS, Cloud SQL, etc.)
- Mount environment variables as Kubernetes secrets
- Liveness probe: `GET /actuator/health`
- Readiness probe: `GET /actuator/health`

---

## Connecting MCP Clients

### Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

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

Restart Claude Desktop. You will see 28 tools available.

### Claude Code (CLI)

```bash
# Add MCP server
claude mcp add mock-ecom --transport sse http://localhost:8080/sse

# List configured servers
claude mcp list

# Remove
claude mcp remove mock-ecom
```

Or edit `~/.claude/settings.json`:

```json
{
  "mcpServers": {
    "mock-ecom": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

### VS Code (Copilot / Continue / Cline)

Add to your VS Code `settings.json`:

```json
{
  "mcp": {
    "servers": {
      "mock-ecom": {
        "type": "sse",
        "url": "http://localhost:8080/sse"
      }
    }
  }
}
```

### Custom MCP Client (Python)

```python
from mcp import ClientSession
from mcp.client.sse import sse_client

async with sse_client("http://localhost:8080/sse") as (read, write):
    async with ClientSession(read, write) as session:
        await session.initialize()
        tools = await session.list_tools()
        result = await session.call_tool("searchProducts", {
            "query": "laptop",
            "page": 0,
            "pageSize": 10
        })
```

### Custom MCP Client (JavaScript/TypeScript)

```typescript
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { SSEClientTransport } from "@modelcontextprotocol/sdk/client/sse.js";

const transport = new SSEClientTransport(
  new URL("http://localhost:8080/sse")
);
const client = new Client({ name: "my-client", version: "1.0.0" }, {});
await client.connect(transport);

const result = await client.callTool("serverToServerLogin", {
  phone: "9999999999",
  platform: "WEB",
  secret: "mock-platform-secret-key"
});
```

### Verifying Connection

```bash
# Test SSE endpoint is alive
curl -N http://localhost:8080/sse

# Health check
curl http://localhost:8080/actuator/health
```

---

## Status & TODOs

### Current Status: ✅ Working

| Feature | Status |
|---------|--------|
| 28 MCP tools exposed via SSE | ✅ Complete |
| Mock data generation (5 categories) | ✅ Complete |
| H2 in-memory dev database | ✅ Complete |
| PostgreSQL production support | ✅ Complete |
| Session-based authentication | ✅ Complete |
| Full shopping flow (search → order) | ✅ Complete |
| Support tickets & comments | ✅ Complete |
| Product & shipment reviews | ✅ Complete |
| Wishlist | ✅ Complete |
| 83 integration tests | ✅ Complete |
| Docker + Docker Compose | ✅ Complete |
| GitHub Actions CI | ✅ Complete |
| One-command local startup script | ✅ Complete |

### TODOs / Future Enhancements

| Priority | Item |
|----------|------|
| High | Add real OTP delivery (SMS/email) via configurable gateway |
| High | Add address creation tool (currently addresses are auto-generated) |
| High | Add product image upload support |
| Medium | Add coupon / discount code tools |
| Medium | Add product inventory management tools |
| Medium | Add admin tools (update order status, resolve tickets) |
| Medium | JWT-based authentication as alternative to platform secret |
| Medium | Swagger / OpenAPI docs for REST endpoints |
| Low | Add Redis caching for product search results |
| Low | Add Elasticsearch for better full-text search |
| Low | Multi-language / multi-currency support |
| Low | Rate limiting on MCP tool calls |
| Low | Webhook notifications for order status changes |
| Low | Export orders as PDF/CSV |

### Known Limitations

- Addresses are not creatable via MCP tools (auto-generated mock addresses are available)
- Payment processing is fully mocked (always succeeds with UPI/CARD/NETBANKING/WALLET, COD always succeeds)
- Delivery OTP is generated but not actually sent anywhere
- Product images are served from `picsum.photos` (external dependency)
- H2 data **persists** across restarts in dev mode (file-based DB in `./data/`). Delete `./data/` to reset.
- Live Swiggy scraping requires unrestricted internet access to `www.swiggy.com`; sandbox/restricted environments must use the committed seed file

---

## Documentation

- [Requirements](docs/REQUIREMENTS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Implementation Guide](docs/IMPLEMENTATION.md)
- [SDLC & Sprint Plan](docs/SDLC.md)
- [Task List (TODO)](docs/TODO.md)
