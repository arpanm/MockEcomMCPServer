# Mock Ecommerce MCP Server — Requirements

## 1. Project Overview

A general-purpose **Mock MCP (Model Context Protocol) Server** built with Java Spring Boot that acts as a backend for AI chatbots connecting to ecommerce platforms. A single AI chatbot using this server can serve users across multiple categories — Grocery, Fashion, Electronics, Beauty, and Home & Kitchen — based on the user's natural language prompt.

### Goals
- Expose **28 MCP tools** that AI chatbot clients can discover and invoke
- Generate **contextual mock data** (product titles, descriptions, images, prices, attributes) based on search keywords
- Persist generated data to a database (check DB first; generate and save asynchronously if missing)
- Support **session-based authentication** via server-to-server login (no OTP from MCP server side)
- Work across all ecommerce verticals from a single server instance

---

## 2. Supported Categories

| Category | Examples |
|---|---|
| **Grocery** | Rice, Dal, Atta, Oil, Ghee, Tea, Biscuits, Spices |
| **Electronics** | Smartphones, Laptops, TVs, Headphones, Cameras, Appliances |
| **Fashion** | Shirts, Jeans, Sarees, Kurtas, Shoes, Bags, Accessories |
| **Beauty** | Moisturizers, Shampoos, Lipstick, Serums, Perfumes |
| **Home** | Bedsheets, Cookware, Furniture, Lighting, Storage |

---

## 3. Authentication Design

### Server-to-Server Login Flow
1. Each ecommerce platform has a **platform secret** configured server-side
2. The AI chatbot server calls `serverToServerLogin(phoneNumber, platform, secret)` → receives a `sessionId` (UUID)
3. The `sessionId` is valid for **24 hours** and stored in the database
4. All subsequent authenticated tool calls include `sessionId` in their parameters
5. If `sessionId` is absent from a tool's parameters, that tool does **not** require login

**Mock platform secret:** `mock-platform-secret-key`

---

## 4. MCP Tools — Full Specification

### 4.1 Product Discovery (No login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 1 | `searchProducts` | `query`, `page`, `pageSize` | Paginated product list |
| 2 | `filterProducts` | `query`, `category`, `subCategory`, `brand`, `page`, `pageSize` | Filtered paginated product list |
| 3 | `getFilters` | `query` | Categories, subcategories, brands with counts |
| 4 | `getSortOptions` | `query` | Available sort keys and labels |
| 5 | `getProductDetails` | `productId` | Full product details + attributes + images |
| 6 | `getProductReviews` | `productId`, `page`, `pageSize` | Rating summary + paginated reviews |
| 7 | `getDeliveryTime` | `productId`, `pincode` | Standard + express delivery options |

### 4.2 Authentication

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 8 | `serverToServerLogin` | `phoneNumber`, `platform`, `secret` | `sessionId`, `customerId`, `expiresAt` |

### 4.3 Cart & Checkout (Login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 9 | `addToCart` | `productId`, `quantity`, `sessionId` | Updated cart with items and total |
| 10 | `checkout` | `cartId`, `sessionId` | Checkout ID and order summary |

### 4.4 Address Management (Login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 11 | `getAddresses` | `sessionId` | List of saved addresses |
| 12 | `selectAddress` | `checkoutId`, `addressId`, `sessionId` | Updated checkout with delivery charges |

### 4.5 Payment (Login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 13 | `initiatePayment` | `checkoutId`, `paymentMethod`, `sessionId` | `paymentId`, status, transaction details |
| 14 | `getPaymentStatus` | `paymentId`, `sessionId` | Payment status + `orderId` if successful |

### 4.6 Order Management (Login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 15 | `getOrderDetails` | `orderId`, `sessionId` | Order with items and shipments |
| 16 | `getOrders` | `sessionId`, `page`, `pageSize` | Paginated order list |
| 17 | `getShipments` | `sessionId`, `page`, `pageSize` | Paginated shipment list |
| 18 | `cancelOrder` | `orderId`, `orderItemId`, `reason`, `sessionId` | Cancellation confirmation + refund info |
| 19 | `returnOrderItem` | `orderItemId`, `reason`, `sessionId` | Return request + pickup schedule |
| 20 | `sendDeliveryOtp` | `shipmentId`, `sessionId` | OTP sent confirmation |

### 4.7 Support Tickets (Login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 21 | `getTickets` | `sessionId`, `page`, `pageSize` | Paginated ticket list |
| 22 | `getTicketDetails` | `ticketId`, `sessionId` | Ticket with all comments |
| 23 | `addTicketComment` | `ticketId`, `comment`, `sessionId` | Added comment + auto agent response |
| 24 | `createTicket` | `subject`, `description`, `type`, `orderId`, `orderItemId`, `sessionId` | Created ticket |

### 4.8 Reviews (Login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 25 | `submitProductReview` | `productId`, `rating`, `title`, `description`, `sessionId` | Submitted review + updated rating |
| 26 | `submitShipmentReview` | `shipmentId`, `rating`, `title`, `description`, `sessionId` | Submitted delivery review |

### 4.9 Wishlist (Login required)

| # | Tool Name | Parameters | Returns |
|---|---|---|---|
| 27 | `addToWishlist` | `productId`, `sessionId` | Wishlist item with product details |
| 28 | `getWishlist` | `sessionId`, `page`, `pageSize` | Paginated wishlist items |

---

## 5. Mock Data Generation Requirements

- **Category detection**: Analyze query keywords to detect grocery/electronics/fashion/beauty/home
- **Contextual data**: Product title, description, brand, model, price, MRP, rating, stock
- **Deterministic**: Same search key always returns the same product (hash-based)
- **Async save**: Generated products are saved to DB asynchronously; DB is checked first
- **Attributes**: Category-specific attributes (size/color for fashion, specs for electronics, etc.)
- **Images**: `https://picsum.photos/seed/{searchKey}/400/400` placeholder images
- **Pricing**: Category-appropriate ranges (Grocery: ₹25–800, Electronics: ₹499–2,00,000, etc.)
- **Delivery zones**: Metro (1-2 days), Tier-2 (3-4 days), Others (5-7 days) based on pincode prefix

---

## 6. Non-Functional Requirements

| Requirement | Specification |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.6 |
| **MCP Protocol** | Spring AI 1.0.0-M6, HTTP/SSE transport |
| **Database (dev)** | H2 in-memory |
| **Database (prod)** | PostgreSQL 16 |
| **Async processing** | Spring `@Async` with thread pool |
| **Session TTL** | 24 hours |
| **Pagination** | Default page size: 10, Max: 50 |
| **Response format** | JSON via `ObjectMapper` |
| **Deployment** | Docker + Docker Compose |
