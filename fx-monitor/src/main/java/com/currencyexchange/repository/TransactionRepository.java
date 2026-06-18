package com.currencyexchange.repository;

import com.currencyexchange.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Transaction> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(Long userId, String type);

    Optional<Transaction> findByTransactionReference(String reference);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByFromWalletId(Long walletId);

    List<Transaction> findByToWalletId(Long walletId);
}
