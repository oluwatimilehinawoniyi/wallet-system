package com.wallet.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "Unique user email address.", example = "jane.doe@example.com")
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,
        @Schema(description = "Plain-text password with a minimum length of 8 characters.", example = "S3curePass!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
        @Schema(description = "User's display or legal full name.", example = "Jane Doe")
        @NotBlank(message = "Full name is required")
        String fullName
) {
}

