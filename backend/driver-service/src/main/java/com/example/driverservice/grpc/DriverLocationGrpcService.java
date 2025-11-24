package com.example.driverservice.grpc;

import com.example.driverservice.service.impl.DriverLocationService;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class DriverLocationGrpcService extends DriverLocationServiceGrpc.DriverLocationServiceImplBase {

    private final DriverLocationService driverLocationService;

    public DriverLocationGrpcService(DriverLocationService driverLocationService) {
        this.driverLocationService = driverLocationService;
    }

    @Override
    public StreamObserver<LocationRequest> sendLocation(StreamObserver<LocationResponse> responseObserver) {
        return new StreamObserver<LocationRequest>() {
            @Override
            public void onNext(LocationRequest locationRequest) {
                driverLocationService.updateLocation(
                        UUID.fromString(locationRequest.getDriverId()),
                        locationRequest.getLatitude(),
                        locationRequest.getLongitude()
                );
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
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
