package com.paymentprovider.currencyservice.provider;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeRateProvider {

    /**
     * Get current exchange rates for a base currency
     * @param baseCurrency The base currency code
     * @return Map of currency codes to exchange rates
     */
    Map<String, BigDecimal> getCurrentRates(String baseCurrency);

    /**
     * Get specific exchange rate between two currencies
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Exchange rate
     */
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);

    /**
     * Get the provider name
     * @return Provider name
     */
    String getProviderName();

    /**
     * Check if the provider is available
     * @return true if available
     */
    boolean isAvailable();
}
