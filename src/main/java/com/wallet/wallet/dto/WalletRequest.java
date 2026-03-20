package com.wallet.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WalletRequest(
        @Schema(description = "Three-letter ISO 4217 currency code for the wallet.",
                example = "NGN", pattern = "^[A-Z]{3}$")
        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$",
                message = "Currency must be a 3-letter ISO code")
        String currency
) {
}

