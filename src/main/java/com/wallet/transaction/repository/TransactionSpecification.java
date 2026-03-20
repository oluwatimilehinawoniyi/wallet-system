package com.wallet.transaction.repository;

import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TransactionStatus;
import com.wallet.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<Transaction> filter(
            UUID walletId,
            TransactionType type,
            TransactionStatus status,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount
    ) {
        List<Specification<Transaction>> filters = new ArrayList<>();
        filters.add(byWalletId(walletId));

        addIfPresent(filters, byType(type));
        addIfPresent(filters, byStatus(status));
        addIfPresent(filters, fromDate(from));
        addIfPresent(filters, toDate(to));
        addIfPresent(filters, minAmount(minAmount));
        addIfPresent(filters, maxAmount(maxAmount));

        return Specification.allOf(filters);
    }

    private static void addIfPresent(List<Specification<Transaction>> filters,
                                     Specification<Transaction> filter) {
        if (filter != null) {
            filters.add(filter);
        }
    }

    private static Specification<Transaction> byWalletId(UUID walletId) {
        return (root, query, cb) -> cb.equal(root.get("walletId"),
                walletId);
    }

    private static Specification<Transaction> byType(
            TransactionType type) {
        return type == null ? null :
                (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> byStatus(
            TransactionStatus status) {
        return status == null ? null :
                (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Transaction> fromDate(LocalDate from) {
        return from == null ? null : (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"),
                        from.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private static Specification<Transaction> toDate(LocalDate to) {
        return to == null ? null : (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"),
                        to.plusDays(1).atStartOfDay()
                                .toInstant(ZoneOffset.UTC).minusNanos(1));
    }

    private static Specification<Transaction> minAmount(
            BigDecimal minAmount) {
        return minAmount == null ? null :
                (root, query, cb) -> cb.greaterThanOrEqualTo(
                        root.get("amount"), minAmount);
    }

    private static Specification<Transaction> maxAmount(
            BigDecimal maxAmount) {
        return maxAmount == null ? null :
                (root, query, cb) -> cb.lessThanOrEqualTo(
                        root.get("amount"), maxAmount);
    }
}
