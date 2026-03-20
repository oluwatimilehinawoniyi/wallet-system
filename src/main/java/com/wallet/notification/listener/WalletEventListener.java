package com.wallet.notification.listener;

import com.wallet.wallet.events.WalletCreatedEvent;
import com.wallet.wallet.events.WalletCreditedEvent;
import com.wallet.wallet.events.WalletDebitedEvent;
import com.wallet.wallet.events.WalletFundedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WalletEventListener {

    @EventListener
    public void onWalletCreated(WalletCreatedEvent event) {
        log.info("Wallet created for user {} with wallet {} in {}",
                event.getUserId(), event.getWalletId(),
                event.getCurrency());
    }

    @EventListener
    public void onWalletFunded(WalletFundedEvent event) {
        log.info("Wallet {} funded with {}", event.getWalletId(),
                event.getAmount());
    }

    @EventListener
    public void onWalletDebited(WalletDebitedEvent event) {
        log.info("Wallet {} debited by {}", event.getWalletId(),
                event.getAmount());
    }

    @EventListener
    public void onWalletCredited(WalletCreditedEvent event) {
        log.info("Wallet {} credited by {}", event.getWalletId(),
                event.getAmount());
    }
}

