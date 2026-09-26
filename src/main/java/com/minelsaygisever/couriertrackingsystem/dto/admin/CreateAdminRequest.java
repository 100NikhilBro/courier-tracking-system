package com.minelsaygisever.couriertrackingsystem.dto.admin;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payload required to register a new Administrator")
public class CreateAdminRequest {

    @NotBlank
    @Schema(description = "Unique username. Must not conflict with existing users.", example = "admin_user")
    private String username;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Secure password (Min 6 chars)", example = "securePass123!")
    private String password;

    @NotBlank
    @Schema(description = "Full name of the administrator", example = "Alice Smith")
    private String fullName;
}
