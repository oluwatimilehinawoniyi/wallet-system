package com.wallet.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBalanceChange(
        UUID walletId,
        UUID userId,
        String currency,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        BigDecimal amount
) {
}
