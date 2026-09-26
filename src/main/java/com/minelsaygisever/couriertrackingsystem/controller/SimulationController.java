package com.minelsaygisever.couriertrackingsystem.controller;

import com.minelsaygisever.couriertrackingsystem.dto.event.CourierLocationEvent;
import com.minelsaygisever.couriertrackingsystem.dto.simulation.JourneyRequest;
import com.minelsaygisever.couriertrackingsystem.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Simulation", description = "Endpoints to simulate courier movements for testing purposes")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/location")
    @Operation(
            summary = "Send Location Update",
            description = "Simulates a courier sending a GPS location update. Pushes data directly to Kafka."
    )
    public String sendLocation(@RequestBody CourierLocationEvent event) {
        simulationService.sendSingleLocation(event);
        return "Event sent to Kafka!";
    }

    @PostMapping("/journey")
    @Operation(
            summary = "Simulate a Journey (Async)",
            description = "Takes a list of coordinates and simulates movement by sending them to Kafka sequentially with a delay. Runs in the background."
    )
    public String simulateJourney(@RequestBody JourneyRequest request) {
        simulationService.startJourney(request);
        return "Journey simulation started for User " + request.getUserId() + "! Check the logs.";
    }

}