# Dockerfile for API Gateway (Spring Boot)

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/*.jar api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
