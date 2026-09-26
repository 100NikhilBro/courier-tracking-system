package com.minelsaygisever.couriertrackingsystem.dto.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Historical record of a courier entering a store's geofence radius")
public class StoreEntryResponse {
    @Schema(description = "Unique identifier of the log entry", example = "101")
    private Long id;

    @Schema(description = "Username of the courier involved", example = "moto1")
    private String courierUsername;

    @Schema(description = "Full name of the courier", example = "John Doe")
    private String courierFullName;

    @Schema(description = "Name of the store visited", example = "Grand Central Supermarket")
    private String storeName;

    @Schema(description = "Exact latitude where the courier was detected", example = "40.9923307")
    private double actualLatitude;

    @Schema(description = "Exact longitude where the courier was detected", example = "29.1244229")
    private double actualLongitude;

    @Schema(description = "Timestamp of the entry event", example = "2023-11-21T14:45:30")
    private LocalDateTime entryTime;
}
