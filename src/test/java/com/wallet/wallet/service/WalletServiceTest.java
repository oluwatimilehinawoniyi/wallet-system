package com.wallet.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserAccountFacade userAccountFacade;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void createWalletPersistsWalletAndPublishesEvent() {
        when(userAccountFacade.requireUserIdByEmail("user@mail.com")).thenReturn(userId);
        when(walletRepository.existsByUserIdAndCurrency(userId, "NGN")).thenReturn(false);
        when(walletRepository.save(org.mockito.ArgumentMatchers.any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            setField(wallet, "id", walletId);
            setAuditField(wallet, "createdAt", Instant.now());
            setAuditField(wallet, "updatedAt", Instant.now());
            return wallet;
        });

        WalletResponse response = walletService.createWallet("user@mail.com", new WalletRequest("ngn"));

        assertEquals(walletId, response.id());
        assertEquals(userId, response.userId());
        assertEquals("NGN", response.currency());
        assertEquals(new BigDecimal("0.0000"), response.balance());
        assertEquals(WalletStatus.ACTIVE, response.status());

        ArgumentCaptor<WalletCreatedEvent> captor = ArgumentCaptor.forClass(WalletCreatedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertEquals(walletId, captor.getValue().getWalletId());
        assertEquals(userId, captor.getValue().getUserId());
    }

    @Test
    void createWalletRejectsDuplicateCurrencyPerUser() {
        when(userAccountFacade.requireUserIdByEmail("user@mail.com")).thenReturn(userId);
        when(walletRepository.existsByUserIdAndCurrency(userId, "NGN")).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> walletService.createWallet("user@mail.com", new WalletRequest("NGN"))
        );
    }

    @Test
    void getBalanceReturnsOwnedWalletBalance() {
        Wallet wallet = wallet();
        when(userAccountFacade.requireUserIdByEmail("user@mail.com")).thenReturn(userId);
        when(walletRepository.findByIdAndUserId(walletId, userId)).thenReturn(Optional.of(wallet));

        BalanceResponse response = walletService.getBalance(walletId, "user@mail.com");

        assertEquals(walletId, response.walletId());
        assertEquals(new BigDecimal("150.0000"), response.balance());
        assertEquals("NGN", response.currency());
    }

    @Test
    void getWalletThrowsWhenWalletDoesNotBelongToCaller() {
        when(userAccountFacade.requireUserIdByEmail("user@mail.com")).thenReturn(userId);
        when(walletRepository.findByIdAndUserId(walletId, userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWallet(walletId, "user@mail.com"));
    }

    private Wallet wallet() {
        Wallet wallet = new Wallet();
        setField(wallet, "id", walletId);
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal("150.0000"));
        wallet.setCurrency("NGN");
        wallet.setStatus(WalletStatus.ACTIVE);
        setAuditField(wallet, "createdAt", Instant.now());
        setAuditField(wallet, "updatedAt", Instant.now());
        return wallet;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setAuditField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
