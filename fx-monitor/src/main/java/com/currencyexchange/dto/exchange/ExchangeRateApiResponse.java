package com.currencyexchange.dto.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRateApiResponse {
    public String base;
    public String date;
    @JsonProperty("time_last_updated")
    public long timeLastUpdated;
    public Map<String, BigDecimal> rates;
}
