# Mock Ecommerce MCP Server — Implementation Guide

## 1. Running Locally

```bash
# Clone the repo
git clone https://github.com/arpanm/MockEcomMCPServer.git
cd MockEcomMCPServer

# Run with Maven (uses H2 in-memory DB by default)
mvn spring-boot:run

# Server starts at http://localhost:8080
# MCP SSE endpoint: http://localhost:8080/sse
# H2 console: http://localhost:8080/h2-console
```

## 2. Running with Docker

```bash
# Build the jar first
mvn clean package -DskipTests

# Run with Docker Compose (includes PostgreSQL)
docker-compose up --build
```

## 3. Connecting an MCP Client

Add this to your MCP client config (e.g. Claude Desktop `claude_desktop_config.json`):

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

## 4. Key Configuration (`application.yml`)

```yaml
app:
  session:
    ttl-hours: 24          # Session validity period
  mock:
    platform-secret: mock-platform-secret-key   # Use this in serverToServerLogin
  pagination:
    default-page-size: 10
    max-page-size: 50
```

## 5. Complete Tool Usage Flow

### Full Shopping Journey

```
1. serverToServerLogin(phone="9876543210", platform="myapp", secret="mock-platform-secret-key")
   → sessionId

2. searchProducts(query="gaming laptop", page=0, pageSize=10)
   → list of products with IDs

3. getProductDetails(productId="<id>")
   → full product + attributes

4. getDeliveryTime(productId="<id>", pincode="110001")
   → delivery options

5. addToCart(productId="<id>", quantity=1, sessionId="<sid>")
   → cartId

6. checkout(cartId="<cid>", sessionId="<sid>")
   → checkoutId

7. getAddresses(sessionId="<sid>")
   → addressId

8. selectAddress(checkoutId="<co>", addressId="<addr>", sessionId="<sid>")
   → updated checkout with grandTotal

9. initiatePayment(checkoutId="<co>", paymentMethod="UPI", sessionId="<sid>")
   → paymentId + orderId

10. getOrderDetails(orderId="<oid>", sessionId="<sid>")
    → full order with shipments
```

## 6. Entity Details

### Product
- `searchKey` (unique) — normalized query key, used as cache key
- `averageRating`, `reviewCount` — updated on review submission
- `additionalImages` — stored as JSON array string

### Session
- `id` is the `sessionId` passed by clients
- `active` + `expiresAt` checked on every validated call

### Order
- `orderNumber` auto-generated as `ORD` + timestamp
- `deliveryAddress` stores a JSON snapshot of address at time of order

### Shipment
- `deliveryOtp` — 6-digit OTP set when `sendDeliveryOtp` is called
- `estimatedDeliveryDate` — set based on pincode zone at order creation

## 7. MockDataGeneratorService Logic

### Category Detection
```
Query → lowercase + strip spaces → check keyword lists:
  GROCERY keywords: rice, dal, atta, oil, ghee, tea, coffee...
  ELECTRONICS keywords: phone, laptop, tv, camera, headphone...
  FASHION keywords: shirt, jeans, saree, kurta, shoes, bag...
  BEAUTY keywords: cream, serum, shampoo, lipstick, moisturizer...
  HOME keywords: pillow, bedsheet, cooker, lamp, sofa...

Highest keyword match score wins. Default: ELECTRONICS
```

### Deterministic Randomness
```java
long seed = Math.abs(searchKey.hashCode());
// All picks use seed + offset to stay deterministic
String brand = brands[(int)(seed % brands.length)];
```

### Price Generation
```
Grocery:     Rs. 25 – 800
Electronics: Rs. 499 – 2,00,000
Fashion:     Rs. 199 – 20,000
Beauty:      Rs. 99 – 5,000
Home:        Rs. 99 – 30,000

Price rounded to nearest x99 or x49 for realistic look.
MRP = price * (1.10 to 1.40)
```

## 8. Error Handling

All tool methods follow this pattern:
```java
try {
    // business logic
    return helper.toJson(result);
} catch (Exception e) {
    log.error("[Tool] methodName error: {}", e.getMessage(), e);
    return helper.error(e.getMessage());
}
```

Error response format:
```json
{"status": "error", "error": "Session not found or expired. Please login again."}
```

## 9. Adding a New Tool

1. Add `@Tool(description="...")` method to appropriate tool class
2. No config changes needed — `MethodToolCallbackProvider` auto-discovers `@Tool` methods
3. Add corresponding service method
4. Add unit test

## 10. Switching to PostgreSQL

```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/mockecomdb
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

# Run with prod profile
mvn spring-boot:run -Dspring.profiles.active=prod
```
