package com.wallet.wallet.service;

import com.wallet.common.events.DomainEventPublisher;
import com.wallet.common.exception.InsufficientBalanceException;
import com.wallet.common.exception.InvalidAmountException;
import com.wallet.common.exception.WalletNotFoundException;
import com.wallet.wallet.api.TransferBalanceChange;
import com.wallet.wallet.api.WalletBalanceChange;
import com.wallet.wallet.api.WalletOperationsFacade;
import com.wallet.wallet.events.WalletCreditedEvent;
import com.wallet.wallet.events.WalletDebitedEvent;
import com.wallet.wallet.events.WalletFundedEvent;
import com.wallet.wallet.model.Wallet;
import com.wallet.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
class WalletOperationsFacadeImpl implements WalletOperationsFacade {

    private static final int MONEY_SCALE = 4;

    private final WalletRepository walletRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public WalletBalanceChange credit(UUID walletId, BigDecimal amount) {
        BigDecimal normalizedAmount = validateAmount(amount);
        Wallet wallet = requireWallet(walletId);
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(normalizedAmount);
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);
        domainEventPublisher.publish(
                new WalletFundedEvent(wallet.getId(), normalizedAmount));
        return toChange(wallet, normalizedAmount, balanceBefore,
                balanceAfter);
    }

    @Override
    public WalletBalanceChange debit(UUID walletId, BigDecimal amount) {
        BigDecimal normalizedAmount = validateAmount(amount);
        Wallet wallet = requireWallet(walletId);
        BigDecimal balanceBefore = wallet.getBalance();
        ensureSufficientBalance(balanceBefore, normalizedAmount);
        BigDecimal balanceAfter = balanceBefore.subtract(normalizedAmount);
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);
        domainEventPublisher.publish(
                new WalletDebitedEvent(wallet.getId(), normalizedAmount));
        return toChange(wallet, normalizedAmount, balanceBefore,
                balanceAfter);
    }

    @Override
    public TransferBalanceChange transfer(UUID sourceWalletId,
                                          UUID destinationWalletId,
                                          BigDecimal amount) {
        BigDecimal normalizedAmount = validateAmount(amount);
        if (sourceWalletId.equals(destinationWalletId)) {
            throw new InvalidAmountException(
                    "Source and destination wallets must be different");
        }

        // Always lock in deterministic UUID order so concurrent opposite-direction transfers do not deadlock.
        UUID firstLockId =
                sourceWalletId.compareTo(destinationWalletId) < 0 ?
                        sourceWalletId : destinationWalletId;
        UUID secondLockId =
                firstLockId.equals(sourceWalletId) ? destinationWalletId :
                        sourceWalletId;
        log.debug("Acquiring transfer locks in order: {} then {}",
                firstLockId, secondLockId);

        Wallet firstLocked = requireWalletWithLock(firstLockId);
        Wallet secondLocked = requireWalletWithLock(secondLockId);
        Wallet sourceWallet =
                firstLocked.getId().equals(sourceWalletId) ? firstLocked :
                        secondLocked;
        Wallet destinationWallet =
                sourceWallet == firstLocked ? secondLocked : firstLocked;

        if (!sourceWallet.getCurrency()
                .equals(destinationWallet.getCurrency())) {
            throw new InvalidAmountException(
                    "Wallet currencies must match for transfer");
        }

        BigDecimal sourceBefore = sourceWallet.getBalance();
        ensureSufficientBalance(sourceBefore, normalizedAmount);
        BigDecimal sourceAfter = sourceBefore.subtract(normalizedAmount);
        sourceWallet.setBalance(sourceAfter);

        BigDecimal destinationBefore = destinationWallet.getBalance();
        BigDecimal destinationAfter =
                destinationBefore.add(normalizedAmount);
        destinationWallet.setBalance(destinationAfter);

        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);

        domainEventPublisher.publish(
                new WalletDebitedEvent(sourceWallet.getId(),
                        normalizedAmount));
        domainEventPublisher.publish(
                new WalletCreditedEvent(destinationWallet.getId(),
                        normalizedAmount));

        return new TransferBalanceChange(
                toChange(sourceWallet, normalizedAmount, sourceBefore,
                        sourceAfter),
                toChange(destinationWallet, normalizedAmount,
                        destinationBefore,
                        destinationAfter)
        );
    }

    @Override
    public UUID requireOwnerId(UUID walletId) {
        return requireWallet(walletId).getUserId();
    }

    private Wallet requireWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found"));
    }

    private Wallet requireWalletWithLock(UUID walletId) {
        return walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found"));
    }

    private WalletBalanceChange toChange(Wallet wallet, BigDecimal amount,
                                         BigDecimal before,
                                         BigDecimal after) {
        return new WalletBalanceChange(wallet.getId(), wallet.getUserId(),
                wallet.getCurrency(), before, after, amount);
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException(
                    "Amount must be greater than zero");
        }

        try {
            return amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidAmountException(
                    "Amount must have at most 4 decimal places");
        }
    }

    private void ensureSufficientBalance(BigDecimal balance,
                                         BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
    }
}
