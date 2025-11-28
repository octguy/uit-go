package com.example.trip_service.service.impl;

import com.example.trip_service.aop.driverAuth.RequireDriver;
import com.example.trip_service.aop.passengerAuth.RequirePassenger;
import com.example.trip_service.aop.userAuth.RequireUser;
import com.example.trip_service.client.DriverClient;
import com.example.trip_service.dto.request.CreateTripRequest;
import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.dto.request.TripNotificationRequest;
import com.example.trip_service.dto.response.EstimateFareResponse;
import com.example.trip_service.dto.response.NearbyDriverResponse;
import com.example.trip_service.dto.response.TripResponse;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.ITripNotificationService;
import com.example.trip_service.service.ITripService;
import com.example.trip_service.util.PricingUtils;
import com.example.trip_service.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import com.example.trip_service.config.DbContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.UUID;
import java.util.List;

@Service
@Slf4j
public class TripServiceImpl implements ITripService {

    private final TripRepository tripRepository;
    private final ITripNotificationService tripNotificationService;
    private final DriverClient driverClient;
    private final TransactionTemplate transactionTemplate;

    public TripServiceImpl(TripRepository tripRepository, 
                          ITripNotificationService tripNotificationService,
                          DriverClient driverClient,
                          PlatformTransactionManager transactionManager) {
        this.tripRepository = tripRepository;
        this.tripNotificationService = tripNotificationService;
        this.driverClient = driverClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @RequireUser
    public UUID getUserId() {
        return SecurityUtil.getCurrentUserId();
    }

    @Override
    @RequirePassenger
    public EstimateFareResponse estimateFare(EstimateFareRequest request) {
        Double distance = PricingUtils.calculateDistanceInKm(request);

        BigDecimal fare = PricingUtils.calculateFareCents(request);

        return EstimateFareResponse.builder()
                .distance(distance)
                .fare(fare)
                .build();
    }

    @Override
    @RequirePassenger
    public TripResponse createTrip(CreateTripRequest request) {
        // Determine Shard
        if (request.getPickupLongitude() >= 102.0) {
            DbContextHolder.setDbType("VN");
        } else {
            DbContextHolder.setDbType("TH");
        }

        try {
            return transactionTemplate.execute(status -> {
                Trip trip = new Trip();

                trip.setPassengerId(SecurityUtil.getCurrentUserId());
                trip.setPickupLatitude(request.getPickupLatitude());
                trip.setPickupLongitude(request.getPickupLongitude());
                trip.setDestinationLatitude(request.getDestinationLatitude());
                trip.setDestinationLongitude(request.getDestinationLongitude());
                trip.setFare(request.getEstimatedFare());
                trip.setStatus(TripStatus.SEARCHING_DRIVER);

                System.out.println("Trip before save: " + trip.getId() + ", request: " + trip.getRequestedAt());

                TripResponse tripResponse = getTripResponse(trip);

                // Get nearby drivers
                List<NearbyDriverResponse> nearbyDrivers = driverClient.getNearbyDrivers(
                        request.getPickupLatitude(),
                        request.getPickupLongitude(),
                        3.0,
                        10
                );

                // Get only the nearest driver (first in the list, sorted by distance)
                List<String> nearbyDriverIds = nearbyDrivers.stream()
                        .limit(1)  // Only take the nearest driver
                        .map(NearbyDriverResponse::getDriverId)
                        .toList();

                // Calculate distance for notification
                EstimateFareRequest estimateFareRequest = new EstimateFareRequest();
                estimateFareRequest.setPickupLatitude(request.getPickupLatitude());
                estimateFareRequest.setPickupLongitude(request.getPickupLongitude());
                estimateFareRequest.setDestinationLatitude(request.getDestinationLatitude());
                estimateFareRequest.setDestinationLongitude(request.getDestinationLongitude());
                Double distanceKm = PricingUtils.calculateDistanceInKm(estimateFareRequest);

                // Publish trip notification to RabbitMQ for nearby drivers
                TripNotificationRequest notification = TripNotificationRequest.builder()
                        .tripId(tripResponse.getId())
                        .passengerId(tripResponse.getPassengerId())
                        .pickupLatitude(request.getPickupLatitude())
                        .pickupLongitude(request.getPickupLongitude())
                        .destinationLatitude(request.getDestinationLatitude())
                        .destinationLongitude(request.getDestinationLongitude())
                        .estimatedFare(request.getEstimatedFare())
                        .distanceKm(distanceKm)
                        .requestedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .nearbyDriverIds(nearbyDriverIds)
                        .build();

                // Only send notification if there are nearby drivers
                if (!nearbyDriverIds.isEmpty()) {
                    tripNotificationService.notifyNearbyDrivers(notification);
                    log.info("Trip {} created and notification sent to RabbitMQ for nearest driver: {}",
                            tripResponse.getId(), nearbyDriverIds.get(0));
                } else {
                    log.warn("Trip {} created but no drivers to notify", tripResponse.getId());
                }

                return tripResponse;
            });
        } finally {
            DbContextHolder.clearDbType();
        }
    }

    @Override
    @RequirePassenger
    public UUID getPassengerId() {
        return SecurityUtil.getCurrentUserId();
    }

    @Override
    @RequireDriver
    public UUID getDriverId() {
        return SecurityUtil.getCurrentUserId();
    }

    @Override
    @RequireUser
    public TripResponse getTripById(UUID id) {
        // Try VN
        DbContextHolder.setDbType("VN");
        try {
            return transactionTemplate.execute(status -> {
                Trip trip = tripRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
                return getTripResponse(trip);
            });
        } catch (Exception e) {
            // Try TH
            DbContextHolder.setDbType("TH");
            try {
                return transactionTemplate.execute(status -> {
                    Trip trip = tripRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
                    return getTripResponse(trip);
                });
            } finally {
                DbContextHolder.clearDbType();
            }
        }
    }

    @Override
    @RequirePassenger
    public TripResponse cancelTrip(UUID id) {
        // Try VN
        DbContextHolder.setDbType("VN");
        try {
            return transactionTemplate.execute(status -> {
                Trip trip = tripRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
                    throw new RuntimeException("Cannot cancel a completed or already cancelled trip");
                }

                trip.setStatus(TripStatus.CANCELLED);
                trip.setCancelledAt(LocalDateTime.now());

                return getTripResponse(trip);
            });
        } catch (Exception e) {
            // Try TH
            DbContextHolder.setDbType("TH");
            try {
                return transactionTemplate.execute(status -> {
                    Trip trip = tripRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                    if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
                        throw new RuntimeException("Cannot cancel a completed or already cancelled trip");
                    }

                    trip.setStatus(TripStatus.CANCELLED);
                    trip.setCancelledAt(LocalDateTime.now());

                    return getTripResponse(trip);
                });
            } finally {
                DbContextHolder.clearDbType();
            }
        }
    }

    @Override
    @RequireDriver
    public TripResponse completeTrip(UUID id) {
        UUID driverId = SecurityUtil.getCurrentUserId();

        // Try VN
        DbContextHolder.setDbType("VN");
        try {
            return transactionTemplate.execute(status -> {
                Trip trip = tripRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                if (trip.getStatus() != TripStatus.IN_PROGRESS) {
                    throw new RuntimeException("Trip is not in progress and cannot be completed");
                }

                if (!trip.getDriverId().equals(driverId)) {
                    throw new RuntimeException("You are not authorized to complete this trip");
                }

                trip.setStatus(TripStatus.COMPLETED);
                trip.setCompletedAt(LocalDateTime.now());

                return getTripResponse(trip);
            });
        } catch (Exception e) {
            // Try TH
            DbContextHolder.setDbType("TH");
            try {
                return transactionTemplate.execute(status -> {
                    Trip trip = tripRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                    if (trip.getStatus() != TripStatus.IN_PROGRESS) {
                        throw new RuntimeException("Trip is not in progress and cannot be completed");
                    }

                    if (!trip.getDriverId().equals(driverId)) {
                        throw new RuntimeException("You are not authorized to complete this trip");
                    }

                    trip.setStatus(TripStatus.COMPLETED);
                    trip.setCompletedAt(LocalDateTime.now());

                    return getTripResponse(trip);
                });
            } finally {
                DbContextHolder.clearDbType();
            }
        }
    }

    @Override
    @RequireDriver
    public TripResponse startTrip(UUID id) {
        UUID driverId = SecurityUtil.getCurrentUserId();

        // Try VN
        DbContextHolder.setDbType("VN");
        try {
            return transactionTemplate.execute(status -> {
                Trip trip = tripRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                if (trip.getStatus() != TripStatus.ACCEPTED) {
                    throw new RuntimeException("Trip is not accepted and cannot be started");
                }

                if (!trip.getDriverId().equals(driverId)) {
                    throw new RuntimeException("You are not authorized to start this trip");
                }

                trip.setStatus(TripStatus.IN_PROGRESS);
                trip.setStartedAt(LocalDateTime.now());

                return getTripResponse(trip);
            });
        } catch (Exception e) {
            // Try TH
            DbContextHolder.setDbType("TH");
            try {
                return transactionTemplate.execute(status -> {
                    Trip trip = tripRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                    if (trip.getStatus() != TripStatus.ACCEPTED) {
                        throw new RuntimeException("Trip is not accepted and cannot be started");
                    }

                    if (!trip.getDriverId().equals(driverId)) {
                        throw new RuntimeException("You are not authorized to start this trip");
                    }

                    trip.setStatus(TripStatus.IN_PROGRESS);
                    trip.setStartedAt(LocalDateTime.now());

                    return getTripResponse(trip);
                });
            } finally {
                DbContextHolder.clearDbType();
            }
        }
    }

    @Override
    @RequireDriver
    public TripResponse acceptTrip(UUID id) {
        UUID driverId = SecurityUtil.getCurrentUserId();

        // Try VN
        DbContextHolder.setDbType("VN");
        try {
            return transactionTemplate.execute(status -> {
                Trip trip = tripRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                if (trip.getStatus() != TripStatus.SEARCHING_DRIVER) {
                    throw new RuntimeException("Trip is not available for acceptance");
                }

                // Check if trip notification has expired (15 seconds TTL)
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expirationTime = trip.getRequestedAt().plusSeconds(15);

                if (now.isAfter(expirationTime)) {
                    throw new RuntimeException("Trip notification has expired. This trip is no longer available for acceptance.");
                }

                trip.setDriverId(driverId);
                trip.setStatus(TripStatus.ACCEPTED);

                return getTripResponse(trip);
            });
        } catch (Exception e) {
            // Try TH
            DbContextHolder.setDbType("TH");
            try {
                return transactionTemplate.execute(status -> {
                    Trip trip = tripRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

                    if (trip.getStatus() != TripStatus.SEARCHING_DRIVER) {
                        throw new RuntimeException("Trip is not available for acceptance");
                    }

                    // Check if trip notification has expired (15 seconds TTL)
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime expirationTime = trip.getRequestedAt().plusSeconds(15);

                    if (now.isAfter(expirationTime)) {
                        throw new RuntimeException("Trip notification has expired. This trip is no longer available for acceptance.");
                    }

                    trip.setDriverId(driverId);
                    trip.setStatus(TripStatus.ACCEPTED);

                    return getTripResponse(trip);
                });
            } finally {
                DbContextHolder.clearDbType();
            }
        }
    }

    @Override
    public List<TripResponse> getAllTrips() {
        List<Trip> allTrips = new ArrayList<>();

        // Get from VN
        DbContextHolder.setDbType("VN");
        try {
            List<Trip> vnTrips = tripRepository.findAll();
            allTrips.addAll(vnTrips);
        } catch (Exception e) {
            log.error("Error fetching trips from VN shard", e);
        }

        // Get from TH
        DbContextHolder.setDbType("TH");
        try {
            List<Trip> thTrips = tripRepository.findAll();
            allTrips.addAll(thTrips);
        } catch (Exception e) {
            log.error("Error fetching trips from TH shard", e);
        } finally {
            DbContextHolder.clearDbType();
        }

        return allTrips.stream().map(this::getTripResponse).toList();
    }

    private TripResponse getTripResponse(Trip trip) {
        trip = tripRepository.save(trip);

        return TripResponse.builder()
                .id(trip.getId())
                .passengerId(trip.getPassengerId())
                .driverId(trip.getDriverId())
                .status(trip.getStatus().name())
                .pickupLatitude(trip.getPickupLatitude())
                .pickupLongitude(trip.getPickupLongitude())
                .destinationLatitude(trip.getDestinationLatitude())
                .destinationLongitude(trip.getDestinationLongitude())
                .fare(trip.getFare())
                .requestedAt(trip.getRequestedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .build();
    }
}
