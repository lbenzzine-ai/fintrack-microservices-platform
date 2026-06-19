# FinTrack — Production-Grade Financial Microservices Platform

[![CI](https://github.com/lbenzzine-ai/fintrack-microservices-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/lbenzzine-ai/fintrack-microservices-platform/actions/workflows/ci.yml)
[![Deploy](https://github.com/lbenzzine-ai/fintrack-microservices-platform/actions/workflows/deploy.yml/badge.svg)](https://github.com/lbenzzine-ai/fintrack-microservices-platform/actions/workflows/deploy.yml)
[![Coverage](https://img.shields.io/badge/coverage-80%25+-brightgreen)](https://lbenzzine-ai.github.io/fintrack-microservices-platform/)

**Live demo:** [https://fintrack.ddns.net](https://fintrack.ddns.net) · **Coverage reports:** [GitHub Pages](https://lbenzzine-ai.github.io/fintrack-microservices-platform/)

---

## Overview

FinTrack is a production-grade financial transactions platform demonstrating enterprise microservices architecture across 7 independent Spring Boot services. It handles user registration, multi-account management, fund transfers with saga orchestration, real-time risk assessment, fee computation, and async notifications.

Ships in two JDK flavors — **Java 17** (platform threads) and **Java 21** (virtual threads via Project Loom) — for easy benchmarking of thread model differences under realistic financial workloads.

---

## Architecture

```
Browser → Nginx (SSL) → API Gateway (8080)
                              │
                    ┌─────────┼──────────┐
                    ▼         ▼          ▼
               user-svc  account-svc  transaction-svc
                    │         │          │
                    └─────────┼──────────┘
                              ▼
                    Kafka (9 topics) / RabbitMQ
                              │
                    notification-svc
```

**Services:**
| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Spring Cloud Gateway — JWT auth, rate limiting, circuit breakers |
| eureka-server | 8761 | Service discovery |
| config-server | 8888 | Centralized configuration |
| user-service | 8081 | Registration, auth, JWT issuance |
| account-service | 8082 | Account management, balance, interest |
| transaction-service | 8083 | Transfers, saga orchestration, risk engine, fees |
| notification-service | 8084 | Email/SMS/Push via Kafka/RabbitMQ |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (virtual threads) + Java 17 |
| Framework | Spring Boot 3.2, Spring Cloud Gateway, Eureka, Config Server |
| Messaging | Apache Kafka (9 topics) + RabbitMQ |
| Cache | Redis 7.2 |
| Database | MySQL 8.0 × 4 (one per bounded context) |
| Migrations | Flyway |
| Frontend | Angular 21 + Tailwind CSS 3.3 + Chart.js |
| Observability | Grafana, Prometheus, Zipkin, Kibana, Elasticsearch |
| Infrastructure | Docker Compose, Nginx, Let's Encrypt SSL |
| CI/CD | GitHub Actions → Oracle Cloud |

---

## Patterns Implemented

- **Saga** — distributed transactions with automatic compensation
- **Circuit Breaker** — Resilience4j on gateway routes + service clients
- **Rate Limiter** — Redis-backed, 10 req/s per user, 20 burst
- **Retry** — exponential backoff on inter-service calls
- **Strategy** — pluggable fee tiers + notification channels
- **Risk Engine** — parallel evaluation of 7 risk rules per transaction
- **Flyway** — versioned schema migrations with checksum validation

---

## Quick Start

> ⚠️ **Requires ~16GB RAM** for the full Docker Compose stack.

```bash
# Clone the repo
git clone https://github.com/lbenzzine-ai/fintrack-microservices-platform
cd fintrack-microservices-platform

# Build all Java 21 services
cd java21
mvn package -DskipTests -q
cd ..

# Start the full stack
docker compose up -d

# Wait ~60s for all services to register with Eureka
# Then open http://localhost:4200 (Angular UI)
# Or http://localhost:8080/swagger-ui.html (API)
```

**Default ports:**
| Service | URL |
|---------|-----|
| Angular UI | http://localhost:4200 |
| API Gateway / Swagger | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Grafana | http://localhost:3000 |
| Zipkin | http://localhost:9411 |
| Kibana | http://localhost:5601 |
| Kafdrop | http://localhost:8090 |
| Mailhog | http://localhost:8025 |

---

## Run the Demo Script

Generates 10 end-to-end transfers and lights up Grafana, Kibana and Zipkin:

```bash
# Default — registers alice + bob, seeds balances, runs 10 transfers
./scripts/demo-transfers.sh

# Against production
BASE=https://fintrack.ddns.net ./scripts/demo-transfers.sh

# Custom parameters
TRANSFER_AMOUNT=500 COUNT=20 ./scripts/demo-transfers.sh
```

**What it does:**
1. Registers `alice-{timestamp}` and `bob-{timestamp}` via the API
2. Opens a USD wallet for each
3. Seeds alice with $10,000 and bob with $5,000
4. Issues 10 × `DOMESTIC_TRANSFER` of $50
5. Reports final balances and links to observability tools

---

## Live Demo

**URL:** [https://fintrack.ddns.net](https://fintrack.ddns.net)

**Demo credentials:**
```
Username: alice-1779742602
Password: DemoPass123!
```

**Observability (public):**
- Grafana: http://132.226.145.155:3000
- Zipkin: http://132.226.145.155:9411
- Kibana: http://132.226.145.155:5601
- Kafdrop: http://132.226.145.155:8090

---

## CI/CD

- Push to `dev` → builds Java 17 + Java 21 in parallel, runs tests, generates JaCoCo + PIT mutation reports
- Merge to `main` → full deploy to Oracle Cloud (build JARs + Angular → SSH deploy → health check)
- Coverage reports published to GitHub Pages on every push

**Coverage:** [https://lbenzzine-ai.github.io/fintrack-microservices-platform](https://lbenzzine-ai.github.io/fintrack-microservices-platform)

---

## Project Structure

```
fintrack-microservices-platform/
├── java21/                    # Java 21 stack (virtual threads)
│   ├── api-gateway/
│   ├── config-server/
│   ├── eureka-server/
│   ├── user-service/
│   ├── account-service/
│   ├── transaction-service/
│   └── notification-service/
├── java17/                    # Java 17 stack (platform threads)
├── fintrack-ui/               # Angular 21 frontend
├── scripts/
│   └── demo-transfers.sh      # End-to-end demo script
├── docker-compose.yml
└── .github/workflows/
    ├── ci.yml                 # Build, test, coverage
    └── deploy.yml             # Deploy to Oracle Cloud
```

---

## License

MIT
