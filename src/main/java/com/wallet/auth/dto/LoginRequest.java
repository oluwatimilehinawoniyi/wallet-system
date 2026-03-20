package com.wallet.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Registered user email address.", example = "jane.doe@example.com")
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,
        @Schema(description = "Account password.", example = "S3curePass!")
        @NotBlank(message = "Password is required")
        String password
) {
}

