# Stage 1: Build
FROM maven:3.9.0-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Pre-download dependencies (cache layer)
RUN mvn -B dependency:go-offline

# Now copy the rest of the source
COPY src ./src

# Build application (skip tests)
RUN mvn -B clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /build/target/user-service-*.jar app.jar

RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8081 9001

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8082/api/user-service/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
