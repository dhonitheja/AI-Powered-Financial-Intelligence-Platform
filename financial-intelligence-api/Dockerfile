FROM eclipse-temurin:17-jre-alpine AS runtime

# CRITICAL: Run as non-root user
RUN addgroup -S wealthix && adduser -S wealthix -G wealthix

WORKDIR /app

COPY target/financial-intelligence-api-*.jar app.jar

# Set correct ownership
RUN chown wealthix:wealthix app.jar

USER wealthix

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
    --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
