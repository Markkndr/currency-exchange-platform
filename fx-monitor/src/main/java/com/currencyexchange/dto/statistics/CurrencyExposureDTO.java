package com.currencyexchange.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Net position in a single currency, valued in the portfolio's home currency.
 *
 * {@code netExposure} is the sum of all wallet balances held in this currency
 * (netting / aggregation). {@code valueInHome} is that position converted to the
 * home currency using the latest exchange rate, and is {@code null} when no rate
 * is available for the currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyExposureDTO {
    private String currency;
    private BigDecimal netExposure;
    private BigDecimal reservedAmount;
    private BigDecimal rateToHome;
    private BigDecimal valueInHome;
    private BigDecimal percentOfPortfolio;
}
