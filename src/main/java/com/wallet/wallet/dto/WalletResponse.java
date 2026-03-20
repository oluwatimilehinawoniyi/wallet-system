package com.wallet.wallet.dto;

import com.wallet.wallet.model.WalletStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

