package com.example.driversimulator.simulate;

import com.example.driver_service.grpc.DriverLocationServiceGrpc;
import com.example.driver_service.grpc.LocationRequest;
import com.example.driver_service.grpc.LocationResponse;
import com.example.driversimulator.entity.Point;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DriverRunner {

    private final DriverLocationServiceGrpc.DriverLocationServiceStub asyncStub;

    public DriverRunner(DriverLocationServiceGrpc.DriverLocationServiceStub asyncStub) {
        this.asyncStub = asyncStub;
    }

    public void simulate(String driverId, List<Point> path, long delayMillis) {

        // Response từ server
        StreamObserver<LocationResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(LocationResponse value) {
                System.out.println("Server ACK: " + value.getAllFields());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("gRPC stream error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed for driver " + driverId);
            }
        };

        // Mở stream để gửi dữ liệu
        StreamObserver<LocationRequest> requestObserver =
                asyncStub.sendLocation(responseObserver);

        new Thread(() -> {
            try {
                for (Point p : path) {

                    LocationRequest req = LocationRequest.newBuilder()
                            .setDriverId(driverId)
                            .setLatitude(p.latitude())
                            .setLongitude(p.longitude())
                            .setTimestamp(System.currentTimeMillis())
                            .build();

                    requestObserver.onNext(req);

                    System.out.println("Sent: " + p.latitude() + "/" + p.longitude());

                    Thread.sleep(delayMillis);
                }

                requestObserver.onCompleted();

            } catch (Exception e) {
                requestObserver.onError(e);
            }
        }).start();
    }
}
