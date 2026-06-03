package com.currencyexchange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @Column(nullable = false)
    private String transactionType; // EXCHANGE, DEPOSIT, WITHDRAWAL, TRANSFER

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal fromAmount;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal toAmount;

    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRateUsed;

    @Column(precision = 19, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED

    @Column(unique = true)
    private String transactionReference;

    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
