package com.paymentprovider.paymentservice.services;

import com.paymentprovider.paymentservice.domain.Currency;
import com.paymentprovider.paymentservice.domain.Payment;

import java.math.BigDecimal;

/**
 * Result of payment processing with currency conversion information
 */
public class PaymentProcessingResult {

    private Payment payment;
    private BigDecimal originalAmount;
    private Currency originalCurrency;
    private boolean conversionApplied;
    private BigDecimal convertedAmount;
    private Currency convertedCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal conversionFee;
    private String conversionError;

    // Constructors
    public PaymentProcessingResult() {}

    // Business methods
    public boolean isConversionSuccessful() {
        return conversionApplied && conversionError == null;
    }

    public BigDecimal getFinalAmount() {
        return conversionApplied ? convertedAmount : originalAmount;
    }

    public Currency getFinalCurrency() {
        return conversionApplied ? convertedCurrency : originalCurrency;
    }

    // Getters and setters
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public Currency getOriginalCurrency() { return originalCurrency; }
    public void setOriginalCurrency(Currency originalCurrency) { this.originalCurrency = originalCurrency; }

    public boolean isConversionApplied() { return conversionApplied; }
    public void setConversionApplied(boolean conversionApplied) { this.conversionApplied = conversionApplied; }

    public BigDecimal getConvertedAmount() { return convertedAmount; }
    public void setConvertedAmount(BigDecimal convertedAmount) { this.convertedAmount = convertedAmount; }

    public Currency getConvertedCurrency() { return convertedCurrency; }
    public void setConvertedCurrency(Currency convertedCurrency) { this.convertedCurrency = convertedCurrency; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public BigDecimal getConversionFee() { return conversionFee; }
    public void setConversionFee(BigDecimal conversionFee) { this.conversionFee = conversionFee; }

    public String getConversionError() { return conversionError; }
    public void setConversionError(String conversionError) { this.conversionError = conversionError; }
}
