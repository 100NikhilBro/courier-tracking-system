package com.minelsaygisever.couriertrackingsystem.dto.courier;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payload required to register a new courier")
public class CreateCourierRequest {

    @NotBlank
    @Schema(description = "Unique username for the courier. Must be unique in the system.", example = "moto_courier_01")
    private String username;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Password for the account (Min 6 chars)", example = "securePass123")
    private String password;

    @NotBlank
    @Schema(description = "Full name of the courier", example = "John Doe")
    private String fullName;
}