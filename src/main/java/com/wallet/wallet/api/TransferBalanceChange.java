package com.wallet.wallet.api;

public record TransferBalanceChange(
        WalletBalanceChange source,
        WalletBalanceChange destination
) {
}
