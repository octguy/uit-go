# Dockerfile for Trip Service (Spring Boot)

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/*.jar trip-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "trip-service.jar"]
