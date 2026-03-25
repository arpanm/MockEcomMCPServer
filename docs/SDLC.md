# Mock Ecommerce MCP Server — SDLC

## 1. Project Phases

| Phase | Description | Duration |
|---|---|---|
| **Phase 1: Foundation** | Project setup, entities, repositories | Sprint 1 |
| **Phase 2: Core Features** | Mock data generator, auth, product search | Sprint 2-3 |
| **Phase 3: Commerce Flow** | Cart, checkout, payment, order | Sprint 4-5 |
| **Phase 4: Support Features** | Reviews, wishlist, tickets | Sprint 6 |
| **Phase 5: Testing & QA** | Unit tests, integration tests, MCP protocol tests | Sprint 7 |
| **Phase 6: DevOps** | Docker, CI/CD, PostgreSQL, monitoring | Sprint 8 |

## 2. Sprint Breakdown

### Sprint 1 — Foundation (Weeks 1-2)
- [ ] Initialize Spring Boot project with Maven
- [ ] Configure Spring AI MCP Server (WebMVC SSE)
- [ ] Create all 16 JPA entities
- [ ] Create all 16 repository interfaces
- [ ] Set up H2 dev + PostgreSQL prod profiles
- [ ] Set up GitHub Actions CI

### Sprint 2 — Mock Data Engine (Weeks 3-4)
- [ ] Implement `MockDataGeneratorService` with category detection
- [ ] Implement keyword-to-category mapping for all 5 categories
- [ ] Implement deterministic product generation
- [ ] Implement async save with `@Async`
- [ ] Implement `ProductService` with search, filter, detail
- [ ] Implement tools 1-7 (product discovery + delivery)
- [ ] Unit test MockDataGeneratorService

### Sprint 3 — Authentication (Weeks 5-6)
- [ ] Implement `AuthService` with server-to-server login
- [ ] Implement session validation and TTL
- [ ] Implement `AddressService` with mock address generation
- [ ] Implement tool 8 (serverToServerLogin)
- [ ] Implement tools 11-12 (getAddresses, selectAddress)
- [ ] Unit test AuthService

### Sprint 4 — Cart & Checkout (Weeks 7-8)
- [ ] Implement `CartService` (add, recalculate)
- [ ] Implement `CheckoutService` (initiate, select address)
- [ ] Implement delivery charge calculation
- [ ] Implement tools 9-10 (addToCart, checkout)
- [ ] Integration test cart flow

### Sprint 5 — Payment & Orders (Weeks 9-10)
- [ ] Implement `PaymentService` with mock payment processing
- [ ] Implement `OrderService` (details, list, cancel, return)
- [ ] Implement shipment creation and OTP
- [ ] Implement tools 13-20
- [ ] Integration test full purchase flow

### Sprint 6 — Support Features (Weeks 11-12)
- [ ] Implement `ReviewService` (product + delivery reviews)
- [ ] Implement `WishlistService`
- [ ] Implement `TicketService` with auto-responses
- [ ] Implement tools 21-28
- [ ] Unit test all services

### Sprint 7 — Testing & QA (Weeks 13-14)
- [ ] Full unit test coverage (target: 80%+)
- [ ] Integration tests for all 28 tool flows
- [ ] MCP protocol validation tests
- [ ] Load testing with mock clients
- [ ] Bug fixes

### Sprint 8 — DevOps (Weeks 15-16)
- [ ] Finalize Docker + Docker Compose setup
- [ ] PostgreSQL migration testing
- [ ] GitHub Actions CD pipeline
- [ ] Health checks and actuator endpoints
- [ ] Production readiness review

## 3. Git Branching Strategy

```
main              ← stable, production-ready
├── develop         ← integration branch
├── feature/*       ← individual features
├── bugfix/*        ← bug fixes
└── release/*       ← release candidates
```

## 4. Testing Strategy

| Layer | Type | Tool |
|---|---|---|
| Services | Unit tests with mocks | JUnit 5 + Mockito |
| Repositories | Integration tests | `@DataJpaTest` + H2 |
| Tools | End-to-end tests | `@SpringBootTest` |
| MCP Protocol | Protocol compliance | Custom MCP test client |

## 5. CI/CD Pipeline

```
Push to branch
    │
    ▼
GitHub Actions
    ├── mvn clean verify (compile + test)
    ├── Docker build
    └── Push image to registry (main branch only)
```

## 6. Environment Strategy

| Environment | DB | Profile | Notes |
|---|---|---|---|
| Local Dev | H2 in-memory | default | Starts fresh each run |
| Test | H2 in-memory | test | Isolated per test class |
| Staging | PostgreSQL | prod | Persistent data |
| Production | PostgreSQL | prod | HA setup |

## 7. Code Standards

- Java 21 features (records, switch expressions, var)
- Lombok for boilerplate reduction
- `@RequiredArgsConstructor` for constructor injection
- `@Slf4j` for logging (never `System.out.println`)
- `@Transactional` on write service methods
- All tool methods return `String` (JSON)
- Error responses use `{"status": "error", "error": "message"}` format
