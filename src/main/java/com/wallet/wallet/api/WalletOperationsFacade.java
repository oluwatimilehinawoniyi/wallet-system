package com.wallet.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletOperationsFacade {

    WalletBalanceChange credit(UUID walletId, BigDecimal amount);

    WalletBalanceChange debit(UUID walletId, BigDecimal amount);

    TransferBalanceChange transfer(UUID sourceWalletId, UUID destinationWalletId, BigDecimal amount);

    UUID requireOwnerId(UUID walletId);
}
