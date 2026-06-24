package com.currencyexchange.dto.transactions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;
    private Long userId;
    private Long fromWalletId;
    private Long toWalletId;
    private String transactionType;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal exchangeRateUsed;
    private BigDecimal feeAmount;
    private String status;
    private String transactionReference;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
