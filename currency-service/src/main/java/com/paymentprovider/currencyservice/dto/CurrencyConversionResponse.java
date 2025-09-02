package com.paymentprovider.currencyservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CurrencyConversionResponse {

    private BigDecimal originalAmount;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private BigDecimal conversionFee;
    private BigDecimal totalAmount;
    private Instant rateDate;
    private String rateProvider;

    // Constructors
    public CurrencyConversionResponse() {}

    public CurrencyConversionResponse(BigDecimal originalAmount, String fromCurrency, String toCurrency,
                                      BigDecimal exchangeRate, BigDecimal convertedAmount) {
        this.originalAmount = originalAmount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.exchangeRate = exchangeRate;
        this.convertedAmount = convertedAmount;
    }

    // Getters and setters
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public BigDecimal getConvertedAmount() { return convertedAmount; }
    public void setConvertedAmount(BigDecimal convertedAmount) { this.convertedAmount = convertedAmount; }

    public BigDecimal getConversionFee() { return conversionFee; }
    public void setConversionFee(BigDecimal conversionFee) { this.conversionFee = conversionFee; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Instant getRateDate() { return rateDate; }
    public void setRateDate(Instant rateDate) { this.rateDate = rateDate; }

    public String getRateProvider() { return rateProvider; }
    public void setRateProvider(String rateProvider) { this.rateProvider = rateProvider; }
}
