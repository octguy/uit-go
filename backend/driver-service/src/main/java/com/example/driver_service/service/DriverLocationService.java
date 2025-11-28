package com.example.driverservice.service;

import com.example.driverservice.dto.NearbyDriverResponse;
import com.example.driverservice.repository.RedisDriverRepository;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DriverLocationService {

    private final RedisDriverRepository redisDriverRepository;

    public DriverLocationService(RedisDriverRepository redisDriverRepository) {
        this.redisDriverRepository = redisDriverRepository;
    }

    public void updateDriverLocation(String driverId, double latitude, double longitude) {
        redisDriverRepository.updateLocation(driverId, latitude, longitude);
    }

    public List<NearbyDriverResponse> findNearbyDrivers(
            double latitude,
            double longitude,
            double radiusKm,
            int limit
    ) {

        // 🚀 Dùng GEOSEARCH API mới: luôn trả về có tọa độ + distance
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisDriverRepository.findNearbyDrivers(latitude, longitude, radiusKm, limit);

        List<NearbyDriverResponse> list = new ArrayList<>();
        if (results == null) return list;

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> r : results) {

            RedisGeoCommands.GeoLocation<String> loc = r.getContent();

            // Luôn có tọa độ vì đã dùng includeCoordinates()
            Point p = loc.getPoint();
            if (p == null) continue; // fallback an toàn

            double distMeters = 0;
            distMeters = r.getDistance().getValue() * 1000;

            list.add(new NearbyDriverResponse(
                    loc.getName(),
                    p.getY(),  // latitude
                    p.getX(),  // longitude
                    distMeters
            ));

            if (list.size() >= limit) break;
        }

        return list;
    }
}
