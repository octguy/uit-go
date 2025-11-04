# Dockerfile for Driver Service (Spring Boot)

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/*.jar driver-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "driver-service.jar"]
