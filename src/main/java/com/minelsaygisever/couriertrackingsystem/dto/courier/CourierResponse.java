package com.minelsaygisever.couriertrackingsystem.dto.courier;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Detailed profile information of a courier")
public class CourierResponse {

    @Schema(description = "Unique identifier of the courier profile", example = "10")
    private Long id;

    @Schema(description = "Associated User ID (used for authentication linkage)", example = "5")
    private Long userId;

    @Schema(description = "Username used for login", example = "moto1")
    private String username;

    @Schema(description = "Full name of the courier", example = "Speedy Gonzales")
    private String fullName;

    @Schema(description = "Account status (true: active, false: soft deleted/banned)", example = "true")
    private boolean enabled;

    @Schema(description = "Cumulative distance traveled by the courier", example = "12500.5")
    private Double totalDistanceInMeters;

    @Schema(description = "Profile creation timestamp", example = "2023-11-20T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last profile update timestamp", example = "2023-11-21T15:30:00")
    private LocalDateTime updatedAt;
}
