# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    if [ -f mvnw ]; then ./mvnw -q -DskipTests package; \
    else mvn -q -DskipTests package; fi || true

FROM eclipse-temurin:17-jre-jammy
LABEL org.opencontainers.image.title="fintrack-transaction-service"

ENV APP_HOME=/app
WORKDIR $APP_HOME

RUN groupadd -r fintrack && useradd -r -g fintrack fintrack \
 && mkdir -p $APP_HOME/logs && chown -R fintrack:fintrack $APP_HOME

COPY --from=build /workspace/target/transaction-service.jar app.jar

USER fintrack
EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8083/actuator/health/liveness || exit 1

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
