package com.paymentprovider.currencyservice.controller;

import com.paymentprovider.currencyservice.domain.Currency;
import com.paymentprovider.currencyservice.dto.CurrencyConversionRequest;
import com.paymentprovider.currencyservice.dto.CurrencyConversionResponse;
import com.paymentprovider.currencyservice.dto.ExchangeRateResponse;
import com.paymentprovider.currencyservice.service.CurrencyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currency")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /**
     * Convert currency amount
     */
    @PostMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Valid @RequestBody CurrencyConversionRequest request) {

        CurrencyConversionResponse response = currencyService.convertCurrency(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current exchange rate between two currencies
     */
    @GetMapping("/rates/{fromCurrency}/{toCurrency}")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @PathVariable String fromCurrency,
            @PathVariable String toCurrency) {

        ExchangeRateResponse response = currencyService.getCurrentExchangeRate(fromCurrency, toCurrency);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get all supported currencies
     */
    @GetMapping("/supported")
    public ResponseEntity<List<Currency>> getSupportedCurrencies() {
        List<Currency> currencies = currencyService.getSupportedCurrencies();
        return ResponseEntity.ok(currencies);
    }

    /**
     * Trigger manual exchange rate update
     */
    @PostMapping("/rates/update")
    public ResponseEntity<Void> updateExchangeRates() {
        currencyService.updateExchangeRates();
        return ResponseEntity.ok().build();
    }
}
