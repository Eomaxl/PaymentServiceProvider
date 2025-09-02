package com.paymentprovider.currencyservice.dto;


import java.math.BigDecimal;
import java.time.Instant;

public class ExchangeRateResponse {

    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private Instant rateDate;
    private String sourceProvider;

    // Constructors
    public ExchangeRateResponse() {}

    public ExchangeRateResponse(String fromCurrency, String toCurrency, BigDecimal rate, Instant rateDate) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
    }

    // Getters and setters
    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public Instant getRateDate() { return rateDate; }
    public void setRateDate(Instant rateDate) { this.rateDate = rateDate; }

    public String getSourceProvider() { return sourceProvider; }
    public void setSourceProvider(String sourceProvider) { this.sourceProvider = sourceProvider; }
}
