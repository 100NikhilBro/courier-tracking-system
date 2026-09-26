package com.minelsaygisever.couriertrackingsystem.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Public profile details of an Administrator")
public class AdminResponse {

    @Schema(description = "Unique identifier of the admin profile", example = "1")
    private Long id;

    @Schema(description = "Associated User ID (used for authentication linkage)", example = "10")
    private Long userId;

    @Schema(description = "Username used for login", example = "superadmin")
    private String username;

    @Schema(description = "Full display name", example = "System Administrator")
    private String fullName;

    @Schema(description = "Account status (true: active, false: deactivated)", example = "true")
    private boolean enabled;

    @Schema(description = "Profile creation timestamp", example = "2023-11-20T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last profile update timestamp", example = "2023-11-21T15:30:00")
    private LocalDateTime updatedAt;
}
