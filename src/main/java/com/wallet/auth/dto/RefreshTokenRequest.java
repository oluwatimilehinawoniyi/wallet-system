package com.wallet.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "Refresh token previously issued by the authentication service.", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}

