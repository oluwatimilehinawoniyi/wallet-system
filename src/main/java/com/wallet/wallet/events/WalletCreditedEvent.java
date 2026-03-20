package com.wallet.wallet.events;

import com.wallet.common.events.BaseEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class WalletCreditedEvent extends BaseEvent {

    private final UUID walletId;
    private final BigDecimal amount;
}

