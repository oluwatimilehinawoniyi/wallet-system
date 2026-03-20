package com.wallet.transaction.dto;

import com.wallet.transaction.model.TransactionStatus;
import com.wallet.transaction.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String reference,
        UUID walletId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        TransactionStatus status,
        String description,
        UUID relatedTransactionId,
        Instant createdAt
) {
}
