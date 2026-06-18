package com.currencyexchange.controller;

import com.currencyexchange.dto.exchange.ConversionResultDTO;
import com.currencyexchange.dto.exchange.ExchangeRateDTO;
import com.currencyexchange.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/exchange-rates")
public class ExchangeRateController {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @GetMapping("/{base}")
    public ResponseEntity<ExchangeRateDTO> getRates(@PathVariable String base) {
        return ResponseEntity.ok(exchangeRateService.getRates(base.toUpperCase()));
    }

    @GetMapping("/convert")
    public ResponseEntity<ConversionResultDTO> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {
        ExchangeRateDTO rates = exchangeRateService.getRates(from.toUpperCase());
        BigDecimal rate = rates.getRates().get(to.toUpperCase());
        if (rate == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(ConversionResultDTO.builder()
                .from(from.toUpperCase())
                .to(to.toUpperCase())
                .amount(amount)
                .rate(rate)
                .convertedAmount(amount.multiply(rate).setScale(4, RoundingMode.HALF_UP))
                .date(rates.getDate())
                .build());
    }
}
