package com.example.driversimulator.entity;

import java.util.ArrayList;
import java.util.List;

public class PathGenerator {

    public static List<Point> generateLinearPath(
            double startLat, double startLng,
            double endLat, double endLng,
            int steps) {

        List<Point> points = new ArrayList<>();

        for (int i = 0; i <= steps; i++) {
            double ratio = i / (double) steps;
            double lat = startLat + (endLat - startLat) * ratio;
            double lng = startLng + (endLng - startLng) * ratio;
            points.add(new Point(lat, lng));
        }

        return points;
    }
}
