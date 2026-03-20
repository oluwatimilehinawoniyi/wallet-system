package com.wallet.transaction.service;

import com.wallet.auth.api.UserAccountFacade;
import com.wallet.common.aop.Idempotent;
import com.wallet.common.events.DomainEventPublisher;
import com.wallet.common.exception.DuplicateTransactionException;
import com.wallet.common.exception.TransactionAlreadyReversedException;
import com.wallet.common.exception.WalletNotFoundException;
import com.wallet.common.response.ApiResponse;
import com.wallet.transaction.dto.FundRequest;
import com.wallet.transaction.dto.TransactionHistoryResponse;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.dto.WithdrawRequest;
import com.wallet.transaction.events.TransactionReversedEvent;
import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TransactionStatus;
import com.wallet.transaction.model.TransactionType;
import com.wallet.transaction.repository.TransactionRepository;
import com.wallet.transaction.repository.TransactionSpecification;
import com.wallet.wallet.api.TransferBalanceChange;
import com.wallet.wallet.api.WalletBalanceChange;
import com.wallet.wallet.api.WalletOperationsFacade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletOperationsFacade walletOperationsFacade;
    private final UserAccountFacade userAccountFacade;
    private final DomainEventPublisher domainEventPublisher;

    @Idempotent
    public ApiResponse<TransactionResponse> fundWallet(
            UUID walletId,
            FundRequest request,
            String email) {
        ensureWalletOwnedByCaller(walletId, email);
        WalletBalanceChange change =
                walletOperationsFacade.credit(walletId, request.amount());
        Transaction transaction = recordTransaction(
                change.walletId(),
                TransactionType.CREDIT,
                request.amount(),
                change.balanceBefore(),
                change.balanceAfter(),
                request.description(),
                null
        );
        return ApiResponse.success("Wallet funded successfully",
                toResponse(transaction));
    }

    @Idempotent
    public ApiResponse<TransactionResponse> withdrawFunds(UUID walletId,
                                                          WithdrawRequest request,
                                                          String email) {
        ensureWalletOwnedByCaller(walletId, email);
        WalletBalanceChange change =
                walletOperationsFacade.debit(walletId, request.amount());
        Transaction transaction = recordTransaction(
                change.walletId(),
                TransactionType.DEBIT,
                request.amount(),
                change.balanceBefore(),
                change.balanceAfter(),
                request.description(),
                null
        );
        return ApiResponse.success("Wallet debited successfully",
                toResponse(transaction));
    }

    @Idempotent
    public ApiResponse<TransactionResponse> transferFunds(
            TransferRequest request, String email) {
        ensureWalletOwnedByCaller(request.sourceWalletId(), email);
        TransferBalanceChange change = walletOperationsFacade.transfer(
                request.sourceWalletId(),
                request.destinationWalletId(),
                request.amount()
        );

        // Transfers produce two ledger entries,
        // so each wallet keeps its own auditable balance trail.
        Transaction sourceTransaction = recordTransaction(
                change.source().walletId(),
                TransactionType.DEBIT,
                request.amount(),
                change.source().balanceBefore(),
                change.source().balanceAfter(),
                request.description(),
                null
        );

        recordTransaction(
                change.destination().walletId(),
                TransactionType.CREDIT,
                request.amount(),
                change.destination().balanceBefore(),
                change.destination().balanceAfter(),
                "Transfer from " + change.source().walletId(),
                sourceTransaction.getId()
        );

        return ApiResponse.success("Transfer completed successfully",
                toResponse(sourceTransaction));
    }

    @Transactional(readOnly = true)
    public ApiResponse<TransactionHistoryResponse> getTransactionHistory(
            UUID walletId,
            int page,
            int size,
            TransactionType type,
            TransactionStatus status,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String email
    ) {
        ensureWalletOwnedByCaller(walletId, email);
        Page<Transaction> transactions = transactionRepository.findAll(
                TransactionSpecification.filter(walletId, type, status,
                        from, to, minAmount, maxAmount),
                PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<TransactionResponse> content =
                transactions.getContent().stream().map(this::toResponse)
                        .toList();
        TransactionHistoryResponse response =
                new TransactionHistoryResponse(
                        content,
                        transactions.getTotalElements(),
                        transactions.getTotalPages(),
                        transactions.getNumber(),
                        transactions.getSize()
                );
        return ApiResponse.success(
                "Transaction history fetched successfully", response);
    }

    @Idempotent
    public ApiResponse<TransactionResponse> reverseTransaction(
            UUID transactionId, String email) {
        Transaction original =
                transactionRepository.findById(transactionId)
                        .orElseThrow(() -> new WalletNotFoundException(
                                "Transaction not found"));

        ensureWalletOwnedByCaller(original.getWalletId(), email);
        if (original.getStatus() != TransactionStatus.SUCCESS) {
            throw new TransactionAlreadyReversedException(
                    "Only successful transactions can be reversed");
        }
        if (transactionRepository.existsByRelatedTransactionId(
                original.getId())) {
            throw new TransactionAlreadyReversedException(
                    "Transaction has already been reversed");
        }

        // Reversal applies the opposite balance mutation first,
        // then records a new immutable ledger entry.
        WalletBalanceChange change =
                original.getType() == TransactionType.CREDIT
                        ? walletOperationsFacade.debit(
                        original.getWalletId(), original.getAmount())
                        : walletOperationsFacade.credit(
                        original.getWalletId(), original.getAmount());

        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        Transaction reversal = recordTransaction(
                original.getWalletId(),
                original.getType() == TransactionType.CREDIT ?
                        TransactionType.DEBIT : TransactionType.CREDIT,
                original.getAmount(),
                change.balanceBefore(),
                change.balanceAfter(),
                "Reversal of " + original.getReference(),
                original.getId()
        );

        domainEventPublisher.publish(
                new TransactionReversedEvent(original.getId(),
                        reversal.getId(), original.getWalletId()));
        return ApiResponse.success("Transaction reversed successfully",
                toResponse(reversal));
    }

    private void ensureWalletOwnedByCaller(UUID walletId, String email) {
        UUID callerId = userAccountFacade.requireUserIdByEmail(email);
        UUID ownerId = walletOperationsFacade.requireOwnerId(walletId);
        if (!callerId.equals(ownerId)) {
            throw new WalletNotFoundException("Wallet not found");
        }
    }

    private Transaction recordTransaction(
            UUID walletId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String description,
            UUID relatedTransactionId
    ) {
        Transaction transaction = new Transaction();
        transaction.setReference(generateReference());
        transaction.setWalletId(walletId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);
        transaction.setRelatedTransactionId(relatedTransactionId);
        return transactionRepository.save(transaction);
    }

    private String generateReference() {
        String reference = "TXN-" + UUID.randomUUID();
        if (transactionRepository.existsByReference(reference)) {
            throw new DuplicateTransactionException(
                    "Duplicate transaction reference generated");
        }
        return reference;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getWalletId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getRelatedTransactionId(),
                transaction.getCreatedAt()
        );
    }
}
