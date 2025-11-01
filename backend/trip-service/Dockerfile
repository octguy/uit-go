# Dockerfile for User Service (Spring Boot)

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/*.jar user-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "user-service.jar"]
