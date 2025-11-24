package com.example.driversimulator.controller;

import com.example.driversimulator.entity.PathGenerator;
import com.example.driversimulator.entity.Point;
import com.example.driversimulator.simulate.MultiDriverSimulator;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulate")
public class SimulatorController {

    private final MultiDriverSimulator multiDriverSimulator;

    public SimulatorController(MultiDriverSimulator multiDriverSimulator) {
        this.multiDriverSimulator = multiDriverSimulator;
    }

    @PostMapping("/start-all")
    public String startAllDrivers(@RequestParam double startLat,
                                  @RequestParam double startLng,
                                  @RequestParam double endLat,
                                  @RequestParam double endLng,
                                  @RequestParam(defaultValue = "100") int steps,
                                  @RequestParam(defaultValue = "1000") long delayMillis) {

        // Gen path chung cho tất cả tài xế
        List<Point> path = PathGenerator.generateLinearPath(
                startLat, startLng,
                endLat, endLng,
                steps
        );

        multiDriverSimulator.simulateAllDrivers(path, delayMillis);

        return "Started simulation for ALL drivers";
    }
}
