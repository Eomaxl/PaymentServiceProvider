package com.paymentprovider.paymentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Embeddable class representing merchant risk profile
 */
@Embeddable
public class RiskProfile {

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }

    @NotNull(message = "Risk level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @NotNull(message = "Daily transaction limit is required")
    @DecimalMin(value = "0.00", message = "Daily transaction limit must be non-negative")
    @Column(name = "daily_transaction_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyTransactionLimit;

    @NotNull(message = "Monthly transaction limit is required")
    @DecimalMin(value = "0.00", message = "Monthly transaction limit must be non-negative")
    @Column(name = "monthly_transaction_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyTransactionLimit;

    @NotNull(message = "Single transaction limit is required")
    @DecimalMin(value = "0.01", message = "Single transaction limit must be greater than 0")
    @Column(name = "single_transaction_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal singleTransactionLimit;

    @Min(value = 0, message = "Chargeback threshold must be non-negative")
    @Max(value = 100, message = "Chargeback threshold must not exceed 100")
    @Column(name = "chargeback_threshold_percentage", nullable = false)
    private Integer chargebackThresholdPercentage;

    @Column(name = "requires_manual_review", nullable = false)
    private Boolean requiresManualReview;

    @Column(name = "fraud_monitoring_enabled", nullable = false)
    private Boolean fraudMonitoringEnabled;

    @Size(max = 500, message = "Risk notes must not exceed 500 characters")
    @Column(name = "risk_notes", length = 500)
    private String riskNotes;

    // Constructors
    public RiskProfile() {
        this.requiresManualReview = false;
        this.fraudMonitoringEnabled = true;
        this.chargebackThresholdPercentage = 1; // 1% default
    }

    public RiskProfile(RiskLevel riskLevel, BigDecimal dailyLimit, BigDecimal monthlyLimit, BigDecimal singleLimit) {
        this();
        this.riskLevel = riskLevel;
        this.dailyTransactionLimit = dailyLimit;
        this.monthlyTransactionLimit = monthlyLimit;
        this.singleTransactionLimit = singleLimit;
    }

    // Business methods
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.VERY_HIGH;
    }

    public boolean exceedsLimit(BigDecimal amount) {
        return amount.compareTo(singleTransactionLimit) > 0;
    }

    // Getters and setters
    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getDailyTransactionLimit() {
        return dailyTransactionLimit;
    }

    public void setDailyTransactionLimit(BigDecimal dailyTransactionLimit) {
        this.dailyTransactionLimit = dailyTransactionLimit;
    }

    public BigDecimal getMonthlyTransactionLimit() {
        return monthlyTransactionLimit;
    }

    public void setMonthlyTransactionLimit(BigDecimal monthlyTransactionLimit) {
        this.monthlyTransactionLimit = monthlyTransactionLimit;
    }

    public BigDecimal getSingleTransactionLimit() {
        return singleTransactionLimit;
    }

    public void setSingleTransactionLimit(BigDecimal singleTransactionLimit) {
        this.singleTransactionLimit = singleTransactionLimit;
    }

    public Integer getChargebackThresholdPercentage() {
        return chargebackThresholdPercentage;
    }

    public void setChargebackThresholdPercentage(Integer chargebackThresholdPercentage) {
        this.chargebackThresholdPercentage = chargebackThresholdPercentage;
    }

    public Boolean getRequiresManualReview() {
        return requiresManualReview;
    }

    public void setRequiresManualReview(Boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }

    public Boolean getFraudMonitoringEnabled() {
        return fraudMonitoringEnabled;
    }

    public void setFraudMonitoringEnabled(Boolean fraudMonitoringEnabled) {
        this.fraudMonitoringEnabled = fraudMonitoringEnabled;
    }

    public String getRiskNotes() {
        return riskNotes;
    }

    public void setRiskNotes(String riskNotes) {
        this.riskNotes = riskNotes;
    }
}
