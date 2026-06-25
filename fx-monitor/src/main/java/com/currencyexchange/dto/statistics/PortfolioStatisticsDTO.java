package com.currencyexchange.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated portfolio statistics for a user: net exposure per currency together
 * with the total portfolio value expressed in a single home currency.
 *
 * {@code unvaluedCurrencies} lists currencies that were held but could not be
 * valued because the rate provider returned no rate for them; their balances are
 * therefore excluded from {@code totalValueInHome}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioStatisticsDTO {
    private String homeCurrency;
    private BigDecimal totalValueInHome;
    private int currencyCount;
    private List<CurrencyExposureDTO> exposures;
    private List<String> unvaluedCurrencies;
}
