package com.minelsaygisever.couriertrackingsystem.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourierLocationEvent {
    private Long userId;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
}