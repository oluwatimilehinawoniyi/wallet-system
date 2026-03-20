package com.wallet.wallet.service;

import com.wallet.auth.api.UserAccountFacade;
import com.wallet.common.events.DomainEventPublisher;
import com.wallet.common.exception.BadRequestException;
import com.wallet.common.exception.WalletNotFoundException;
import com.wallet.wallet.dto.BalanceResponse;
import com.wallet.wallet.dto.WalletRequest;
import com.wallet.wallet.dto.WalletResponse;
import com.wallet.wallet.events.WalletCreatedEvent;
import com.wallet.wallet.model.Wallet;
import com.wallet.wallet.model.WalletStatus;
import com.wallet.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final int BALANCE_SCALE = 4;
    private static final BigDecimal ZERO_BALANCE =
            BigDecimal.ZERO.setScale(BALANCE_SCALE,
                    RoundingMode.UNNECESSARY);

    private final WalletRepository walletRepository;
    private final UserAccountFacade userAccountFacade;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public WalletResponse createWallet(String email,
                                       WalletRequest request) {
        UUID userId = userAccountFacade.requireUserIdByEmail(email);

        String currency =
                request.currency().trim().toUpperCase(Locale.ROOT);
        if (walletRepository.existsByUserIdAndCurrency(userId,
                currency)) {
            throw new BadRequestException(
                    "Wallet already exists for this currency");
        }

        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(ZERO_BALANCE);
        wallet.setCurrency(currency);
        wallet.setStatus(WalletStatus.ACTIVE);
        Wallet savedWallet = walletRepository.save(wallet);

        domainEventPublisher.publish(
                new WalletCreatedEvent(savedWallet.getId(), userId,
                        savedWallet.getCurrency()));
        return toResponse(savedWallet);
    }

    public WalletResponse getWallet(UUID walletId, String email) {
        return toResponse(getOwnedWallet(walletId, email));
    }

    public BalanceResponse getBalance(UUID walletId, String email) {
        Wallet wallet = getOwnedWallet(walletId, email);
        return new BalanceResponse(wallet.getId(), wallet.getBalance(),
                wallet.getCurrency());
    }

    private Wallet getOwnedWallet(UUID walletId, String email) {
        UUID userId = userAccountFacade.requireUserIdByEmail(email);
        return walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found"));
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
