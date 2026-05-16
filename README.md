# FinTrack — Production-Grade Fintech Microservices

A side-by-side **Java 17** and **Java 21** implementation of a financial-transactions
platform built with Spring Boot 3.x, Spring Cloud, Kafka/RabbitMQ choreography,
MySQL, Redis, EHCache (Hibernate L2), Resilience4j, ELK, Prometheus/Grafana, and
Zipkin tracing.

The Java 21 variant additionally demonstrates **virtual threads**, **records**,
**sealed interfaces**, **pattern matching**, and a **map-based strategy registry**
for O(1) strategy dispatch.

---

## Repository Layout

```
fintrack/
├── java17/                       Spring Boot 3.2.x, JDK 17 (platform threads, Lombok @Data, interfaces)
│   ├── pom.xml                   Parent POM (fintrack-parent:1.0.0-jdk17)
│   ├── config-server/            :8888 — Spring Cloud Config Server
│   ├── eureka-server/            :8761 — Service Discovery
│   ├── api-gateway/              :8080 — Gateway, JWT, CB, Rate Limit, Swagger aggregator
│   ├── user-service/             :8081 — Users, JWT auth, Redis cache, Flyway
│   ├── account-service/          :8082 — Balances, EHCache L2, Saga participant
│   ├── transaction-service/      :8083 — Saga orchestrator, 100k seed, Strategy fees
│   └── notification-service/     :8084 — Async multi-channel notifications
├── java21/                       Spring Boot 3.3.x, JDK 21 (virtual threads, records, sealed, pattern matching)
│   └── ... (identical service list, modernised internals)
├── postman/                      Postman collection + environments
├── scripts/                      bootstrap.sh, seed.sh, smoke.sh, build helpers
├── docs/                         Architecture notes, ADRs, sequence diagrams
├── docker-compose.yml            Full infra stack (MySQL ×4, Kafka, RabbitMQ, Redis, ELK, Zipkin, Prometheus, Grafana)
├── Makefile                      Top-level workflow shortcuts
└── README.md
```

## Service Topology

| Service               | Port | DB Port | Responsibilities                                                          |
|-----------------------|------|---------|---------------------------------------------------------------------------|
| config-server         | 8888 | —       | Centralised config served from native FS / Git                            |
| eureka-server         | 8761 | —       | Netflix Eureka service registry                                           |
| api-gateway           | 8080 | —       | Reactive gateway, JWT filter, Redis rate limit, R4J CB, Swagger aggregator |
| user-service          | 8081 | 3307    | Auth (JWT), users, roles. Emits `user-registered`                         |
| account-service       | 8082 | 3308    | Balances, EHCache L2, interest Strategy. Consumes/emits saga events       |
| transaction-service   | 8083 | 3309    | Saga orchestrator. Fee Strategy. Emits `transaction-*`                    |
| notification-service  | 8084 | 3310    | Channel Strategy (Email/SMS/Push/Slack). Async. Consumes all events       |

## Saga Choreography (Kafka topics)

```
user-registered ──▶ account-service (auto-create wallet) ──▶ account-created
                                                                │
client ──▶ tx-service (initiate) ──▶ transaction-initiated ─────┤
                                                                ▼
                                                       account-service debits
                                                                │
                                                       account-debited ──▶ tx-service
                                                                │
                                          ┌─── success ─▶ transaction-completed
                                          │
                                          └─── failure ─▶ transaction-failed ──▶ account compensate
```

## Patterns Implemented

- **Strategy Pattern** — replaces every `switch` for fees, notifications, brokers, interest, compensation.
- **Saga (Choreography)** — Kafka topics drive distributed transactions across services.
- **Circuit Breaker** — Resilience4j with explicit CLOSED/OPEN/HALF-OPEN states.
- **Caching** — Hibernate L1 (session), L2 (EHCache READ_WRITE / READ_ONLY), Query Cache, Redis @Cacheable.
- **Messaging Abstraction** — `MessagingStrategy` swaps between Kafka and RabbitMQ via `fintrack.messaging.broker`.
- **MapStruct** mappers everywhere (Entity ⇄ DTO).
- **Flyway** migrations per service DB.
- **OpenAPI 3** with JWT auth, aggregated at the gateway.

## Quick Start

```bash
make infra-up         # docker-compose: MySQL ×4, Kafka, RabbitMQ, Redis, Zipkin, Prom/Graf, ELK
make build-17         # build the Java 17 stack
make run-17           # run all services (Java 17)
make seed             # populate transaction-service with 100k+ rows
make smoke            # run smoke tests
make stop             # stop everything

make build-21 / make run-21   # Java 21 equivalents
```

UI endpoints once up:

- Eureka:        http://localhost:8761
- Gateway:       http://localhost:8080
- Swagger:       http://localhost:8080/swagger-ui.html
- Kafdrop:       http://localhost:8090
- RabbitMQ:      http://localhost:15672 (guest/guest)
- Zipkin:        http://localhost:9411
- Prometheus:    http://localhost:9090
- Grafana:       http://localhost:3000 (admin/admin)
- Kibana:        http://localhost:5601

## Java 17 vs Java 21 — Where the Versions Diverge

| Concern              | Java 17                                     | Java 21                                          |
|----------------------|---------------------------------------------|--------------------------------------------------|
| DTOs                 | `@Data @Builder` Lombok classes             | `record` types                                   |
| Strategy hierarchy   | `interface FeeStrategy`                     | `sealed interface FeeStrategy permits ...`       |
| Strategy registry    | `Map<String, FeeStrategy>` (Spring-injected)| `Map<FeeType, FeeStrategy>` enum-keyed, O(1)     |
| Type checks          | `instanceof X` + explicit cast              | `instanceof X x` pattern binding                 |
| Branching            | `if/else if`                                | switch expressions                               |
| Threading            | Tomcat platform pool (200)                  | `spring.threads.virtual.enabled=true`            |
| Spring Boot          | 3.2.5                                       | 3.3.5                                            |

See `docs/JAVA21-DIVERGENCE.md` for line-by-line examples.
