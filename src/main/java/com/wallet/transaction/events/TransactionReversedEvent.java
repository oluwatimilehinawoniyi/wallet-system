package com.wallet.transaction.events;

import com.wallet.common.events.BaseEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TransactionReversedEvent extends BaseEvent {

    private final UUID originalTransactionId;
    private final UUID reversalTransactionId;
    private final UUID walletId;

    public TransactionReversedEvent(
            UUID originalTransactionId,
            UUID reversalTransactionId,
            UUID walletId) {
        this.originalTransactionId = originalTransactionId;
        this.reversalTransactionId = reversalTransactionId;
        this.walletId = walletId;
    }
}
