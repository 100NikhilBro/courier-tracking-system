package com.minelsaygisever.couriertrackingsystem.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierLastLocation implements Serializable {
    private double latitude;
    private double longitude;
    private long timestamp;
}