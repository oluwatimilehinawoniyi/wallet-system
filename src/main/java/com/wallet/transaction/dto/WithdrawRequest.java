package com.wallet.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WithdrawRequest(
        @Schema(description = "Amount to debit from the wallet. Must be at least 0.01.", example = "75.50", minimum = "0.01")
        @NotNull(message = "Amount is required")
        @Digits(integer = 15, fraction = 4,
                message = "Amount must have at most 4 decimal places")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,
        @Schema(description = "Short narrative describing the withdrawal.", example = "ATM cash-out")
        @NotBlank(message = "Description is required")
        String description
) {
}
