package com.currencyexchange.dto.exchange;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ExchangeRateDTO {
    private String base;
    private String date;
    private long lastUpdated;
    private Map<String, BigDecimal> rates;
}
