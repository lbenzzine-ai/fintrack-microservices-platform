FROM eclipse-temurin:21-jre-jammy
ENV APP_HOME=/app
WORKDIR $APP_HOME
RUN groupadd -r fintrack && useradd -r -g fintrack fintrack \
 && mkdir -p $APP_HOME/logs && chown -R fintrack:fintrack $APP_HOME
COPY target/api-gateway.jar app.jar
USER fintrack
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
