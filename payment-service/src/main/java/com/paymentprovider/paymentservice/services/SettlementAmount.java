package com.paymentprovider.paymentservice.services;

import com.paymentprovider.paymentservice.domain.Currency;

import java.math.BigDecimal;

/**
 * Settlement amount information with currency conversion details
 */
public class SettlementAmount {

    private String paymentId;
    private BigDecimal originalAmount;
    private Currency originalCurrency;
    private BigDecimal settlementAmount;
    private Currency settlementCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal conversionFee;

    // Constructors
    public SettlementAmount() {}

    // Business methods
    public boolean requiresConversion() {
        return !originalCurrency.equals(settlementCurrency);
    }

    public BigDecimal getNetSettlementAmount() {
        return settlementAmount.subtract(conversionFee != null ? conversionFee : BigDecimal.ZERO);
    }

    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public Currency getOriginalCurrency() { return originalCurrency; }
    public void setOriginalCurrency(Currency originalCurrency) { this.originalCurrency = originalCurrency; }

    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(BigDecimal settlementAmount) { this.settlementAmount = settlementAmount; }

    public Currency getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(Currency settlementCurrency) { this.settlementCurrency = settlementCurrency; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public BigDecimal getConversionFee() { return conversionFee; }
    public void setConversionFee(BigDecimal conversionFee) { this.conversionFee = conversionFee; }
}
