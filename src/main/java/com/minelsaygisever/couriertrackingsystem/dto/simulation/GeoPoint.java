package com.minelsaygisever.couriertrackingsystem.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {
    @Schema(example = "40.9923307")
    private double latitude;

    @Schema(example = "29.1244229")
    private double longitude;
}