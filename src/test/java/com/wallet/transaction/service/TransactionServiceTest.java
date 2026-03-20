package com.wallet.transaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wallet.auth.api.UserAccountFacade;
import com.wallet.common.events.DomainEventPublisher;
import com.wallet.common.exception.DuplicateTransactionException;
import com.wallet.common.exception.TransactionAlreadyReversedException;
import com.wallet.common.response.ApiResponse;
import com.wallet.transaction.dto.FundRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.events.TransactionReversedEvent;
import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TransactionStatus;
import com.wallet.transaction.model.TransactionType;
import com.wallet.transaction.repository.TransactionRepository;
import com.wallet.wallet.api.WalletBalanceChange;
import com.wallet.wallet.api.WalletOperationsFacade;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletOperationsFacade walletOperationsFacade;

    @Mock
    private UserAccountFacade userAccountFacade;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    private UUID walletId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void fundWalletThrowsWhenGeneratedReferenceAlreadyExists() {
        when(userAccountFacade.requireUserIdByEmail("user@mail.com")).thenReturn(userId);
        when(walletOperationsFacade.requireOwnerId(walletId)).thenReturn(userId);
        when(walletOperationsFacade.credit(walletId, new BigDecimal("50.00"))).thenReturn(
                new WalletBalanceChange(
                        walletId,
                        userId,
                        "NGN",
                        new BigDecimal("10.00"),
                        new BigDecimal("60.00"),
                        new BigDecimal("50.00")
                )
        );
        when(transactionRepository.existsByReference(anyString())).thenReturn(true);

        assertThrows(
                DuplicateTransactionException.class,
                () -> transactionService.fundWallet(
                        walletId,
                        new FundRequest(new BigDecimal("50.00"), "Funding"),
                        "user@mail.com"
                )
        );
    }

    @Test
    void reverseTransactionCreatesOppositeEntryAndPublishesEvent() {
        Transaction original = transaction(
                UUID.randomUUID(),
                walletId,
                "TXN-1",
                TransactionType.DEBIT,
                TransactionStatus.SUCCESS,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                new BigDecimal("80.00"),
                null
        );

        when(transactionRepository.findById(original.getId())).thenReturn(Optional.of(original));
        when(userAccountFacade.requireUserIdByEmail("user@mail.com")).thenReturn(userId);
        when(walletOperationsFacade.requireOwnerId(walletId)).thenReturn(userId);
        when(transactionRepository.existsByRelatedTransactionId(original.getId())).thenReturn(false);
        when(walletOperationsFacade.credit(walletId, original.getAmount())).thenReturn(
                new WalletBalanceChange(
                        walletId,
                        userId,
                        "NGN",
                        new BigDecimal("80.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("20.00")
                )
        );
        when(transactionRepository.existsByReference(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                setId(tx, UUID.randomUUID());
                if (tx.getCreatedAt() == null) {
                    setCreatedAt(tx, Instant.now());
                }
            }
            return tx;
        });

        ApiResponse<TransactionResponse> response = transactionService.reverseTransaction(original.getId(), "user@mail.com");

        assertEquals("Transaction reversed successfully", response.message());
        assertEquals(TransactionType.CREDIT, response.data().type());
        assertEquals(TransactionStatus.SUCCESS, response.data().status());
        assertEquals(original.getId(), response.data().relatedTransactionId());
        assertEquals(TransactionStatus.REVERSED, original.getStatus());

        ArgumentCaptor<TransactionReversedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionReversedEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertEquals(original.getId(), eventCaptor.getValue().getOriginalTransactionId());
        assertEquals(walletId, eventCaptor.getValue().getWalletId());
    }

    @Test
    void reverseTransactionRejectsAlreadyReversedTransaction() {
        Transaction original = transaction(
                UUID.randomUUID(),
                walletId,
                "TXN-2",
                TransactionType.CREDIT,
                TransactionStatus.SUCCESS,
                new BigDecimal("15.00"),
                new BigDecimal("40.00"),
                new BigDecimal("55.00"),
                null
        );

        when(transactionRepository.findById(original.getId())).thenReturn(Optional.of(original));
        when(userAccountFacade.requireUserIdByEmail("user@mail.com")).thenReturn(userId);
        when(walletOperationsFacade.requireOwnerId(walletId)).thenReturn(userId);
        when(transactionRepository.existsByRelatedTransactionId(original.getId())).thenReturn(true);

        assertThrows(
                TransactionAlreadyReversedException.class,
                () -> transactionService.reverseTransaction(original.getId(), "user@mail.com")
        );

        verify(walletOperationsFacade, never()).credit(any(), any());
        verify(walletOperationsFacade, never()).debit(any(), any());
    }

    private Transaction transaction(
            UUID id,
            UUID walletId,
            String reference,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal before,
            BigDecimal after,
            UUID relatedTransactionId
    ) {
        Transaction transaction = new Transaction();
        setId(transaction, id);
        setCreatedAt(transaction, Instant.now());
        transaction.setWalletId(walletId);
        transaction.setReference(reference);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(before);
        transaction.setBalanceAfter(after);
        transaction.setDescription("desc");
        transaction.setRelatedTransactionId(relatedTransactionId);
        return transaction;
    }

    private void setId(Transaction transaction, UUID id) {
        try {
            var field = Transaction.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(transaction, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setCreatedAt(Transaction transaction, Instant createdAt) {
        try {
            var field = transaction.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(transaction, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
