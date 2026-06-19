package com.currencyexchange.service;

import com.currencyexchange.dto.exchange.ExchangeRateApiResponse;
import com.currencyexchange.dto.exchange.ExchangeRateDTO;
import com.currencyexchange.exception.ExchangeRateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService")
class ExchangeRateServiceTest {

    private static final String API_URL = "https://rates.test/v4/latest";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        // @Value-injected field is not populated by Mockito; set it explicitly.
        ReflectionTestUtils.setField(exchangeRateService, "apiUrl", API_URL);
    }

    @Test
    @DisplayName("maps the API response and upper-cases the base currency in the request URL")
    void returnsMappedRates() {
        ExchangeRateApiResponse apiResponse = new ExchangeRateApiResponse();
        apiResponse.base = "USD";
        apiResponse.date = "2026-06-19";
        apiResponse.timeLastUpdated = 1_750_000_000L;
        apiResponse.rates = Map.of("EUR", new BigDecimal("0.92"), "GBP", new BigDecimal("0.79"));

        when(restTemplate.getForObject(eq(API_URL + "/USD"), eq(ExchangeRateApiResponse.class)))
                .thenReturn(apiResponse);

        ExchangeRateDTO dto = exchangeRateService.getRates("usd");

        assertThat(dto.getBase()).isEqualTo("USD");
        assertThat(dto.getDate()).isEqualTo("2026-06-19");
        assertThat(dto.getLastUpdated()).isEqualTo(1_750_000_000L);
        assertThat(dto.getRates()).containsEntry("EUR", new BigDecimal("0.92"));
    }

    @Test
    @DisplayName("throws when the API returns a null body")
    void throwsOnNullResponse() {
        when(restTemplate.getForObject(eq(API_URL + "/USD"), eq(ExchangeRateApiResponse.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> exchangeRateService.getRates("USD"))
                .isInstanceOf(ExchangeRateException.class)
                .hasMessageContaining("Empty response");
    }

    @Test
    @DisplayName("throws when the API response has no rates")
    void throwsOnMissingRates() {
        ExchangeRateApiResponse apiResponse = new ExchangeRateApiResponse();
        apiResponse.base = "USD";
        apiResponse.rates = null;
        when(restTemplate.getForObject(eq(API_URL + "/USD"), eq(ExchangeRateApiResponse.class)))
                .thenReturn(apiResponse);

        assertThatThrownBy(() -> exchangeRateService.getRates("USD"))
                .isInstanceOf(ExchangeRateException.class)
                .hasMessageContaining("Empty response");
    }

    @Test
    @DisplayName("wraps a RestClientException in an ExchangeRateException")
    void wrapsRestClientException() {
        when(restTemplate.getForObject(eq(API_URL + "/USD"), eq(ExchangeRateApiResponse.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> exchangeRateService.getRates("USD"))
                .isInstanceOf(ExchangeRateException.class)
                .hasMessageContaining("Failed to fetch exchange rates");
    }
}
