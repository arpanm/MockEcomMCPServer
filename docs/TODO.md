# Mock Ecommerce MCP Server — Task List

This is the master task list for the multi-agent development team.

---

## EPIC-1: Project Setup & Infrastructure

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E1-T1 | Initialize Spring Boot project | Create pom.xml with Spring Boot 3.3.6, Spring AI 1.0.0-M6, H2, PostgreSQL, Lombok dependencies | S | Backend | ✅ Done |
| E1-T2 | Application configuration | Set up application.yml, application-prod.yml, application-test.yml | S | Backend | ✅ Done |
| E1-T3 | Docker setup | Dockerfile + docker-compose.yml with app + PostgreSQL | S | DevOps | ✅ Done |
| E1-T4 | CI/CD pipeline | GitHub Actions workflow for build + test | S | DevOps | ✅ Done |
| E1-T5 | Config beans | McpToolsConfig, AsyncConfig, AppProperties | S | Backend | ✅ Done |

---

## EPIC-2: Database & Entity Layer

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E2-T1 | Product entities | Product + ProductAttribute JPA entities | S | Backend | ✅ Done |
| E2-T2 | Auth entities | Customer + Session entities | S | Backend | ✅ Done |
| E2-T3 | Cart entities | Cart + CartItem entities | S | Backend | ✅ Done |
| E2-T4 | Checkout entity | Checkout entity with CheckoutStatus enum | S | Backend | ✅ Done |
| E2-T5 | Order entities | Order + OrderItem + Shipment entities | M | Backend | ✅ Done |
| E2-T6 | Payment entity | Payment entity with PaymentStatus enum | S | Backend | ✅ Done |
| E2-T7 | Support entities | Address + Review + WishlistItem | S | Backend | ✅ Done |
| E2-T8 | Ticket entities | Ticket + TicketComment entities | S | Backend | ✅ Done |
| E2-T9 | All repositories | 16 JPA repository interfaces with custom queries | M | Backend | ✅ Done |

---

## EPIC-3: Mock Data Generation Engine

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E3-T1 | Category detection | Keyword-to-category mapping for 5 categories (Grocery, Electronics, Fashion, Beauty, Home) | M | Data | ✅ Done |
| E3-T2 | Product generation | Deterministic product data generation (brand, title, description, price, images, attributes) | L | Data | ✅ Done |
| E3-T3 | Async save pipeline | @Async save with thread pool, DB-first lookup pattern | S | Backend | ✅ Done |
| E3-T4 | Attribute generation | Category-specific attribute templates (size/color for fashion, specs for electronics) | M | Data | ✅ Done |

**Acceptance Criteria:**
- Same query always returns the same product (deterministic)
- New queries generate products in <100ms (sync generation)
- Products saved to DB within 5 seconds (async)
- Category detected correctly for 95%+ of common search terms

---

## EPIC-4: Authentication & Session Management

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E4-T1 | AuthService | serverToServerLogin + validateSession + getCustomerFromSession | M | Backend | ✅ Done |
| E4-T2 | AddressService | getAddresses + auto-generate mock Indian addresses | M | Backend | ✅ Done |
| E4-T3 | Auth tool (Tool 8) | serverToServerLogin MCP tool | S | Backend | ✅ Done |

**Acceptance Criteria:**
- Login with correct secret returns valid sessionId (UUID)
- Login with wrong secret returns error
- Expired session returns "Session expired" error
- Sessions persist across server restarts (stored in DB)

---

## EPIC-5: Product Search & Discovery Tools (Tools 1-7)

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E5-T1 | ProductService | searchProducts, filterProducts, getFilters, getProductById, getProductReviews | L | Backend | ✅ Done |
| E5-T2 | Product search tools | Tools 1-4: searchProducts, filterProducts, getFilters, getSortOptions | M | Backend | ✅ Done |
| E5-T3 | Product detail tools | Tools 5-6: getProductDetails, getProductReviews | M | Backend | ✅ Done |
| E5-T4 | Delivery tool | Tool 7: getDeliveryTime with pincode zone detection | M | Backend | ✅ Done |

**Acceptance Criteria:**
- searchProducts generates and returns 8 products for new queries
- filterProducts correctly filters by category/subCategory/brand combinations
- getDeliveryTime returns correct zone (metro 1-2d, tier2 3-4d, others 5-7d)
- All tools return valid JSON strings

---

## EPIC-6: Cart & Checkout Flow Tools (Tools 9-14)

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E6-T1 | CartService | addToCart, recalculateCart, initiateCheckout | M | Backend | ✅ Done |
| E6-T2 | CheckoutService | getCheckout, selectAddress, calculateDelivery | M | Backend | ✅ Done |
| E6-T3 | PaymentService | initiatePayment, createOrderFromCheckout, getPaymentStatus | L | Backend | ✅ Done |
| E6-T4 | Cart/Address/Payment tools | Tools 9-14 MCP tool implementations | M | Backend | ✅ Done |

**Acceptance Criteria:**
- Full flow: login → search → addToCart → checkout → selectAddress → pay → orderId
- Cart total recalculates correctly on item add
- Free delivery for orders >= Rs. 499
- Payment creates Order + Shipment automatically

---

## EPIC-7: Order Management Tools (Tools 15-20)

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E7-T1 | OrderService | getOrderDetails, getOrders, getShipments, cancelOrder, returnOrderItem, sendDeliveryOtp | L | Backend | ✅ Done |
| E7-T2 | Order tools | Tools 15-19: getOrderDetails, getOrders, getShipments, cancelOrder, returnOrderItem | M | Backend | ✅ Done |
| E7-T3 | Shipment tool | Tool 20: sendDeliveryOtp | S | Backend | ✅ Done |

**Acceptance Criteria:**
- Cancel only allowed for non-shipped orders/items
- Return only allowed for DELIVERED items
- OTP is 6 digits, saved to shipment record
- Paginated order list ordered by createdAt DESC

---

## EPIC-8: Support Tickets Tools (Tools 21-24)

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E8-T1 | TicketService | getTickets, getTicketDetails, addComment, createTicket | M | Backend | ✅ Done |
| E8-T2 | Ticket tools | Tools 21-24 MCP tool implementations | M | Backend | ✅ Done |
| E8-T3 | Auto-response | Generate contextual auto-responses per ticket type | S | Data | ✅ Done |

**Acceptance Criteria:**
- PAYMENT_ISSUE tickets get HIGH priority auto-assigned
- createTicket adds a SYSTEM comment with ticket ID confirmation
- addComment adds SUPPORT_AGENT auto-response
- Ticket list sorted by createdAt DESC

---

## EPIC-9: Reviews & Wishlist Tools (Tools 25-28)

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E9-T1 | ReviewService | submitProductReview, submitShipmentReview, updateProductRating | M | Backend | ✅ Done |
| E9-T2 | WishlistService | addToWishlist, getWishlist | S | Backend | ✅ Done |
| E9-T3 | Review + Wishlist tools | Tools 25-28 MCP tool implementations | M | Backend | ✅ Done |

**Acceptance Criteria:**
- Rating validated: must be 1-5
- Product averageRating updated after each product review
- Duplicate wishlist entries prevented (idempotent add)
- Wishlist sorted by addedAt DESC

---

## EPIC-10: Testing & QA

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E10-T1 | MockDataGeneratorService tests | Unit tests for category detection, product generation, attributes | M | QA | ⏳ Pending |
| E10-T2 | Service integration tests | @SpringBootTest tests for full service flows | L | QA | ⏳ Pending |
| E10-T3 | Tool response tests | Validate all 28 tools return valid JSON | M | QA | ⏳ Pending |
| E10-T4 | MCP protocol test | Verify MCP server responds to tools/list and tools/call | M | QA | ⏳ Pending |

---

## EPIC-11: DevOps & Deployment

| ID | Task | Description | Effort | Owner | Status |
|---|---|---|---|---|---|
| E11-T1 | Docker packaging | Finalize Dockerfile + docker-compose.yml | S | DevOps | ✅ Done |
| E11-T2 | PostgreSQL migration | Test full flow with PostgreSQL (prod profile) | M | DevOps | ⏳ Pending |
| E11-T3 | Monitoring | Actuator health/metrics endpoints, optional Prometheus | M | DevOps | ⏳ Pending |

---

## Effort Legend
- **S** = Small (< 2 hours)
- **M** = Medium (2-4 hours)
- **L** = Large (4-8 hours)

## Owner Legend
- **Backend** = Java/Spring Boot developer
- **Data** = Data/ML engineer (mock data generation)
- **DevOps** = Infrastructure/deployment
- **QA** = Test engineer
