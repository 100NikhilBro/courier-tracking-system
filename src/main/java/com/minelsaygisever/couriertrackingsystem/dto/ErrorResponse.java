package com.minelsaygisever.couriertrackingsystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Standard structure for all API error responses")
public class ErrorResponse {
    @Schema(description = "Time when the error occurred", example = "2023-11-21T15:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP Status Code", example = "404")
    private int status;

    @Schema(description = "Short error description", example = "Not Found")
    private String error;

    @Schema(description = "Detailed error message", example = "Courier not found with id: 5")
    private String message;

    @Schema(description = "API path where the error occurred", example = "/api/v1/couriers/5")
    private String path;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(
            description = "Map of field validation errors (only present for 400 Bad Request)",
            example = "{\"username\": \"must not be blank\", \"password\": \"size must be between 6 and 20\"}"
    )
    private Map<String, String> validationErrors;
}