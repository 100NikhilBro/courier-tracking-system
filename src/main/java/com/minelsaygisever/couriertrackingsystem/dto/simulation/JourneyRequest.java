package com.minelsaygisever.couriertrackingsystem.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for simulating a courier journey along a path")
public class JourneyRequest {

    @Schema(description = "The User ID of the courier being simulated", example = "1")
    private Long userId;

    @Schema(description = "Delay in seconds between each location update", example = "2")
    private int delayInSeconds;

    @Schema(description = "List of coordinates representing the path")
    private List<GeoPoint> path;
}