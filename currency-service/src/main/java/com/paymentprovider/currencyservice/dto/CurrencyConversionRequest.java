package com.paymentprovider.currencyservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public class CurrencyConversionRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String fromCurrency;

    @NotBlank
    @Size(min = 3, max = 3)
    private String toCurrency;

    private Instant asOfDate;

    private boolean includeFees = true;

    // Constructors
    public CurrencyConversionRequest() {}

    public CurrencyConversionRequest(BigDecimal amount, String fromCurrency, String toCurrency) {
        this.amount = amount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
    }

    // Getters and setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public Instant getAsOfDate() { return asOfDate; }
    public void setAsOfDate(Instant asOfDate) { this.asOfDate = asOfDate; }

    public boolean isIncludeFees() { return includeFees; }
    public void setIncludeFees(boolean includeFees) { this.includeFees = includeFees; }
}
