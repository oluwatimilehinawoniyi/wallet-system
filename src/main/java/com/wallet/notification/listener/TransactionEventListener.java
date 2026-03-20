package com.wallet.notification.listener;

import com.wallet.transaction.events.TransactionReversedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransactionEventListener {

    @EventListener
    public void onTransactionReversed(TransactionReversedEvent event) {
        log.info(
                "Transaction {} reversed by {} for wallet {}",
                event.getOriginalTransactionId(),
                event.getReversalTransactionId(),
                event.getWalletId()
        );
    }
}
