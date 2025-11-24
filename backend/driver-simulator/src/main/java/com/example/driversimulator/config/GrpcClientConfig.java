package com.example.driversimulator.config;

import com.example.driverservice.grpc.DriverLocationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ManagedChannel channel() {
        return ManagedChannelBuilder
                .forAddress("driver-service", 9092)
                .usePlaintext()
                .build();
    }

    @Bean
    public DriverLocationServiceGrpc.DriverLocationServiceStub locationStub(ManagedChannel channel) {
        return DriverLocationServiceGrpc.newStub(channel);
    }
}
