package com.wallet.wallet.repository;

import com.wallet.wallet.model.Wallet;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") UUID id);

    boolean existsByUserIdAndCurrency(UUID userId, String currency);

    Optional<Wallet> findByIdAndUserId(UUID id, UUID userId);
}

