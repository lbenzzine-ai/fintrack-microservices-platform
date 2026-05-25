# FinTrack — top-level workflow shortcuts
# Two parallel stacks: java17/ and java21/

SHELL        := /bin/bash
COMPOSE      := docker compose
MVN          := ./mvnw -q
JDK17_DIR    := java17
JDK21_DIR    := java21

.PHONY: help \
        infra-up infra-down infra-logs infra-ps \
        build-17 build-21 build-all \
        clean-17 clean-21 clean-all \
        run-17 run-21 stop \
        seed smoke test-17 test-21 \
        format

help:
	@echo "FinTrack ─ Makefile targets"
	@echo ""
	@echo "  infra-up         start docker-compose infrastructure (MySQL x4, Kafka, Rabbit, Redis, ELK, Zipkin, Prom, Grafana)"
	@echo "  infra-down       stop and remove infrastructure containers"
	@echo "  infra-logs       tail logs from infrastructure"
	@echo "  infra-ps         show running infra containers"
	@echo ""
	@echo "  build-17         mvn package the Java 17 stack"
	@echo "  build-21         mvn package the Java 21 stack"
	@echo "  build-all        build both stacks"
	@echo ""
	@echo "  run-17           run all Java 17 services (background)"
	@echo "  run-21           run all Java 21 services (background)"
	@echo "  stop             stop services started by run-* targets"
	@echo ""
	@echo "  seed             seed transaction-service with 100k+ rows"
	@echo "  smoke            run smoke tests against running gateway"
	@echo "  test-17 / test-21  mvn test"
	@echo ""
	@echo "  clean-17 / clean-21 / clean-all  mvn clean"

# ── infrastructure ────────────────────────────────────────────────────────────
infra-up: build-21
	$(COMPOSE) up -d

infra-down:
	$(COMPOSE) down -v

infra-logs:
	$(COMPOSE) logs -f --tail=200

infra-ps:
	$(COMPOSE) ps

# ── build ─────────────────────────────────────────────────────────────────────
build-17:
	cd $(JDK17_DIR) && mvn -q -DskipTests package

build-21:
	cd $(JDK21_DIR) && mvn -q -DskipTests package

build-all: build-17 build-21

# ── clean ─────────────────────────────────────────────────────────────────────
clean-17:
	cd $(JDK17_DIR) && mvn -q clean

clean-21:
	cd $(JDK21_DIR) && mvn -q clean

clean-all: clean-17 clean-21

# ── test ──────────────────────────────────────────────────────────────────────
test-17:
	cd $(JDK17_DIR) && mvn -q test

test-21:
	cd $(JDK21_DIR) && mvn -q test

# ── run / stop (local JVM, requires `make infra-up` first) ────────────────────
run-17:
	./scripts/run-stack.sh $(JDK17_DIR)

run-21:
	./scripts/run-stack.sh $(JDK21_DIR)

stop:
	./scripts/stop-stack.sh

# ── seed / smoke ──────────────────────────────────────────────────────────────
seed:
	./scripts/seed.sh

smoke:
	./scripts/smoke.sh

# ── start (build + run everything) ───────────────────────────────────────────
start-21:
	cd $(JDK21_DIR) && mvn -q -DskipTests package
	$(COMPOSE) up -d

start-17:
	cd $(JDK17_DIR) && mvn -q -DskipTests package
	JAVA_FLAVOR=java17 $(COMPOSE) up -d

stop-all:
	$(COMPOSE) down
