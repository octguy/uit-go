# Required Dependencies for Spring Boot Project

Include these dependencies in your `pom.xml` to support your tech stack:

## Core Dependencies
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-amqp (RabbitMQ)
- spring-boot-starter-grpc (or grpc-spring-boot-starter)
- postgresql

## Additional/Recommended
- spring-boot-starter-actuator (monitoring)
- lombok (optional, for reducing boilerplate)
- spring-boot-devtools (development tools)
- junit-jupiter (testing)

## Example Maven Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>2.14.0.RELEASE</version>
</dependency>
```

Add these dependencies to your `pom.xml` to enable PostgreSQL, RabbitMQ, gRPC, and Spring Boot features.