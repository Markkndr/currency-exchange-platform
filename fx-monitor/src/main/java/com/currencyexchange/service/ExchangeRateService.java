package com.currencyexchange.service;

import com.currencyexchange.dto.exchange.ExchangeRateApiResponse;
import com.currencyexchange.dto.exchange.ExchangeRateDTO;
import com.currencyexchange.exception.ExchangeRateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ExchangeRateService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${forex.api.url:https://api.exchangerate-api.com/v4/latest}")
    private String apiUrl;

    @Cacheable(value = "exchangeRates", key = "#base.toUpperCase()")
    public ExchangeRateDTO getRates(String base) {
        String url = apiUrl + "/" + base.toUpperCase();
        log.info("Fetching exchange rates for base: {}", base.toUpperCase());
        try {
            ExchangeRateApiResponse response = restTemplate.getForObject(url, ExchangeRateApiResponse.class);
            if (response == null || response.rates == null) {
                throw new ExchangeRateException("Empty response from exchange rate API");
            }
            return ExchangeRateDTO.builder()
                    .base(response.base)
                    .date(response.date)
                    .lastUpdated(response.timeLastUpdated)
                    .rates(response.rates)
                    .build();
        } catch (RestClientException e) {
            log.error("Failed to fetch exchange rates for {}: {}", base, e.getMessage());
            throw new ExchangeRateException("Failed to fetch exchange rates: " + e.getMessage());
        }
    }

    @CacheEvict(value = "exchangeRates", allEntries = true)
    @Scheduled(fixedRateString = "#{${forex.api.update-interval:3600} * 1000}")
    public void evictRatesCache() {
        log.info("Exchange rate cache cleared — rates will refresh on next request");
    }
}
