package com.currencyexchange.dto.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequestDTO {

    @NotBlank(message = "Transaction type is required")
    private String transactionType; // EXCHANGE, DEPOSIT, WITHDRAWAL, TRANSFER

    private Long fromWalletId;
    private Long toWalletId;

    @NotNull(message = "From amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "From amount must be greater than zero")
    private BigDecimal fromAmount;

    @NotNull(message = "To amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "To amount must be greater than zero")
    private BigDecimal toAmount;

    private BigDecimal exchangeRateUsed;
    private BigDecimal feeAmount;
    private String description;
}
