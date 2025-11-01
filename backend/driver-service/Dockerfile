# Dockerfile for Driver Service (Spring Boot)

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/*.jar driver-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "driver-service.jar"]
