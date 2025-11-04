# Dockerfile for User Service (Spring Boot)

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/*.jar user-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "user-service.jar"]
