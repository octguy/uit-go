# Dockerfile for API Gateway (Spring Boot)

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/*.jar api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
