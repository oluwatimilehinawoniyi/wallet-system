package com.wallet.transaction.repository;

import com.wallet.transaction.model.Transaction;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    boolean existsByReference(String reference);

    boolean existsByRelatedTransactionId(UUID relatedTransactionId);
}

