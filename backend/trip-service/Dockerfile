# Dockerfile for Trip Service (Spring Boot)

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/*.jar trip-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "trip-service.jar"]
