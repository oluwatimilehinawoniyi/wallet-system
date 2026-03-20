package com.wallet.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @Schema(description = "Identifier of the wallet to debit.",
                example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "Source wallet id is required")
        UUID sourceWalletId,
        @Schema(description = "Identifier of the wallet to credit.",
                example = "123e4567-e89b-12d3-a456-426614174001")
        @NotNull(message = "Destination wallet id is required")
        UUID destinationWalletId,
        @Schema(description = "Amount to transfer. Must be at least 0.01.",
                example = "250.00", minimum = "0.01")
        @NotNull(message = "Amount is required")
        @Digits(integer = 15, fraction = 4,
                message = "Amount must have at most 4 decimal places")
        @DecimalMin(value = "0.01",
                message = "Amount must be at least 0.01")
        BigDecimal amount,
        @Schema(description = "Short narrative describing the transfer.",
                example = "Rent contribution")
        @NotBlank(message = "Description is required")
        String description
) {
}
