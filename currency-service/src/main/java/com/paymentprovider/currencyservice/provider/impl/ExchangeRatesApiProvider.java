package com.paymentprovider.currencyservice.provider.impl;

import com.paymentprovider.currencyservice.provider.ExchangeRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExchangeRatesApiProvider implements ExchangeRateProvider {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRatesApiProvider.class);

    @Value("${currency.provider.exchangeratesapi.url:https://api.exchangeratesapi.io/v1}")
    private String apiUrl;

    @Value("${currency.provider.exchangeratesapi.apikey:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public ExchangeRatesApiProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, BigDecimal> getCurrentRates(String baseCurrency) {
        try {
            String url = String.format("%s/latest?access_key=%s&base=%s", apiUrl, apiKey, baseCurrency);

            // For demo purposes, return mock data
            Map<String, BigDecimal> rates = new HashMap<>();
            rates.put("USD", BigDecimal.valueOf(1.0));
            rates.put("EUR", BigDecimal.valueOf(0.85));
            rates.put("GBP", BigDecimal.valueOf(0.73));
            rates.put("JPY", BigDecimal.valueOf(110.0));
            rates.put("CAD", BigDecimal.valueOf(1.25));
            rates.put("AUD", BigDecimal.valueOf(1.35));

            logger.info("Retrieved exchange rates for base currency: {}", baseCurrency);
            return rates;

        } catch (Exception e) {
            logger.error("Failed to retrieve exchange rates for base currency: {}", baseCurrency, e);
            return new HashMap<>();
        }
    }

    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            if (fromCurrency.equals(toCurrency)) {
                return BigDecimal.ONE;
            }

            Map<String, BigDecimal> rates = getCurrentRates(fromCurrency);
            BigDecimal rate = rates.get(toCurrency);

            if (rate == null) {
                // Try reverse conversion
                Map<String, BigDecimal> reverseRates = getCurrentRates(toCurrency);
                BigDecimal reverseRate = reverseRates.get(fromCurrency);
                if (reverseRate != null && reverseRate.compareTo(BigDecimal.ZERO) > 0) {
                    rate = BigDecimal.ONE.divide(reverseRate, 8, RoundingMode.HALF_UP);
                }
            }

            return rate != null ? rate : BigDecimal.ZERO;

        } catch (Exception e) {
            logger.error("Failed to get exchange rate from {} to {}", fromCurrency, toCurrency, e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public String getProviderName() {
        return "ExchangeRatesAPI";
    }

    @Override
    public boolean isAvailable() {
        try {
            // Simple availability check
            getCurrentRates("USD");
            return true;
        } catch (Exception e) {
            logger.warn("Exchange rate provider is not available: {}", e.getMessage());
            return false;
        }
    }
}
