package com.example.driver_service.grpc;

import com.example.driver_service.service.DriverLocationService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class DriverLocationGrpcService extends DriverLocationServiceGrpc.DriverLocationServiceImplBase {

    private final DriverLocationService driverLocationService;

    public DriverLocationGrpcService(DriverLocationService driverLocationService) {
        this.driverLocationService = driverLocationService;
    }

    @Override
    public StreamObserver<LocationRequest> sendLocation(StreamObserver<LocationResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(LocationRequest locationRequest) {
                driverLocationService.updateDriverLocation(
                        locationRequest.getDriverId(),
                        locationRequest.getLatitude(),
                        locationRequest.getLongitude()
                );
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
                System.err.println("Error in location stream: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                LocationResponse response = LocationResponse.newBuilder()
                        .setStatus("Location updated successfully")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }
}
