package com.minelsaygisever.couriertrackingsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @Schema(description = "Unique username of the user", example = "admin")
    private String username;

    @Schema(description = "User's password", example = "admin123")
    private String password;
}
