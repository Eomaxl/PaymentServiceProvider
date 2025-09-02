package com.paymentprovider.paymentservice.services;

import com.paymentprovider.paymentservice.dto.CurrencyConversionRequest;
import com.paymentprovider.paymentservice.dto.CurrencyConversionResponse;
import com.paymentprovider.paymentservice.dto.ExchangeRateResponse;
import com.paymentprovider.paymentservice.domain.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CurrencyServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyServiceClient.class);

    @Value("${currency.service.url:http://localhost:8083}")
    private String currencyServiceUrl;

    private final RestTemplate restTemplate;

    public CurrencyServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Convert currency amount
     */
    public CurrencyConversionResponse convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        try {
            CurrencyConversionRequest request = new CurrencyConversionRequest();
            request.setAmount(amount);
            request.setFromCurrency(fromCurrency);
            request.setToCurrency(toCurrency);
            request.setIncludeFees(true);

            String url = currencyServiceUrl + "/api/v1/currency/convert";
            ResponseEntity<CurrencyConversionResponse> response = restTemplate.postForEntity(
                    url, request, CurrencyConversionResponse.class);

            logger.debug("Currency conversion successful: {} {} to {} {}",
                    amount, fromCurrency, response.getBody().getTotalAmount(), toCurrency);

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to convert currency from {} to {}: {}", fromCurrency, toCurrency, e.getMessage());
            throw new CurrencyConversionException("Currency conversion failed", e);
        }
    }

    /**
     * Get current exchange rate between two currencies
     */
    public ExchangeRateResponse getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            String url = currencyServiceUrl + "/api/v1/currency/rates/{fromCurrency}/{toCurrency}";
            ResponseEntity<ExchangeRateResponse> response = restTemplate.getForEntity(
                    url, ExchangeRateResponse.class, fromCurrency, toCurrency);

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to get exchange rate from {} to {}: {}", fromCurrency, toCurrency, e.getMessage());
            return null;
        }
    }

    /**
     * Get all supported currencies
     */
    public List<Currency> getSupportedCurrencies() {
        try {
            String url = currencyServiceUrl + "/api/v1/currency/supported";
            ResponseEntity<List<Currency>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Currency>>() {});

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to get supported currencies: {}", e.getMessage());
            throw new CurrencyServiceException("Failed to retrieve supported currencies", e);
        }
    }

    /**
     * Check if currency is supported
     */
    public boolean isCurrencySupported(String currencyCode) {
        try {
            List<Currency> supportedCurrencies = getSupportedCurrencies();
            return supportedCurrencies.stream()
                    .anyMatch(currency -> currency.getCode().equals(currencyCode));
        } catch (Exception e) {
            logger.warn("Failed to check currency support for {}: {}", currencyCode, e.getMessage());
            return false;
        }
    }
}
