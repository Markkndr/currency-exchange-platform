package com.currencyexchange.service;

import com.currencyexchange.dto.transactions.CreateTransactionRequestDTO;
import com.currencyexchange.dto.transactions.TransactionDTO;
import com.currencyexchange.entity.Transaction;
import com.currencyexchange.entity.User;
import com.currencyexchange.entity.Wallet;
import com.currencyexchange.exception.InvalidTransactionException;
import com.currencyexchange.exception.TransactionNotFoundException;
import com.currencyexchange.repository.TransactionRepository;
import com.currencyexchange.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserService userService;

    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserTransactionsByType(Long userId, String type) {
        return transactionRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(userId, type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a single transaction, enforcing that it belongs to the requesting
     * user. A transaction owned by someone else is reported as not found so we
     * never leak the existence of other users' transactions.
     */
    @Transactional(readOnly = true)
    public TransactionDTO getUserTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser() != null && userId.equals(t.getUser().getId()))
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + transactionId));
        return toDTO(transaction);
    }

    @Transactional
    public TransactionDTO createTransaction(Long userId, CreateTransactionRequestDTO request) {
        User user = userService.getUserById(userId);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setFromWallet(resolveOwnedWallet(userId, request.getFromWalletId()));
        transaction.setToWallet(resolveOwnedWallet(userId, request.getToWalletId()));
        transaction.setTransactionType(request.getTransactionType().toUpperCase());
        transaction.setFromAmount(request.getFromAmount());
        transaction.setToAmount(request.getToAmount());
        transaction.setExchangeRateUsed(request.getExchangeRateUsed());
        transaction.setFeeAmount(request.getFeeAmount() != null ? request.getFeeAmount() : BigDecimal.ZERO);
        transaction.setStatus("PENDING");
        transaction.setTransactionReference(generateReference());
        transaction.setDescription(request.getDescription());

        transaction = transactionRepository.save(transaction);
        log.info("Transaction {} created for user {}", transaction.getTransactionReference(), userId);

        return toDTO(transaction);
    }

    /**
     * Resolves a wallet by id and verifies it belongs to the requesting user.
     * A null id means "no wallet on this side" (e.g. an external deposit).
     * A wallet that doesn't exist or isn't owned by the user is rejected so a
     * caller can't attach another user's wallet to their transaction.
     */
    private Wallet resolveOwnedWallet(Long userId, Long walletId) {
        if (walletId == null) {
            return null;
        }
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new InvalidTransactionException("Invalid wallet: " + walletId));
        if (wallet.getUser() == null || !userId.equals(wallet.getUser().getId())) {
            throw new InvalidTransactionException("Invalid wallet: " + walletId);
        }
        return wallet;
    }

    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private TransactionDTO toDTO(Transaction t) {
        return TransactionDTO.builder()
                .id(t.getId())
                .userId(t.getUser() != null ? t.getUser().getId() : null)
                .fromWalletId(t.getFromWallet() != null ? t.getFromWallet().getId() : null)
                .toWalletId(t.getToWallet() != null ? t.getToWallet().getId() : null)
                .transactionType(t.getTransactionType())
                .fromAmount(t.getFromAmount())
                .toAmount(t.getToAmount())
                .exchangeRateUsed(t.getExchangeRateUsed())
                .feeAmount(t.getFeeAmount())
                .status(t.getStatus())
                .transactionReference(t.getTransactionReference())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
