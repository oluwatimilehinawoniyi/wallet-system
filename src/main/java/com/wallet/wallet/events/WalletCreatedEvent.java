package com.wallet.wallet.events;

import com.wallet.common.events.BaseEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class WalletCreatedEvent extends BaseEvent {

    private final UUID walletId;
    private final UUID userId;
    private final String currency;
}

