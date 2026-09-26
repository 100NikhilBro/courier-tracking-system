package com.minelsaygisever.couriertrackingsystem.dto.courier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payload for updating an existing courier. All fields are optional (partial update).")
public class UpdateCourierRequest {

    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "New password. Leave null to keep current password.", example = "newPass123")
    private String password;

    @Schema(description = "New full name. Leave null to keep current name.", example = "John Updated")
    private String fullName;

    @Schema(description = "Update account status. Set 'false' to deactivate (Soft Delete).", example = "true")
    private Boolean enabled;
}