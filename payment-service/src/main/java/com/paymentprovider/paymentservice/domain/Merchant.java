package com.paymentprovider.paymentservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

/**
 * Merchant entity representing a merchant account
 */
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @NotBlank
    @Column(name = "business_name", nullable = false)
    private String businessName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MerchantStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_currency", nullable = false)
    private com.paymentprovider.paymentservice.domain.Currency settlementCurrency;

    @Column(name = "auto_currency_conversion")
    private Boolean autoCurrencyConversion = true;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentMethodConfig> paymentMethods;

    @Embedded
    private RiskProfile riskProfile;

    @Embedded
    private SettlementConfig settlementConfig;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public Merchant() {}

    public Merchant(String merchantId, String businessName, com.paymentprovider.paymentservice.domain.Currency settlementCurrency) {
        this.merchantId = merchantId;
        this.businessName = businessName;
        this.settlementCurrency = settlementCurrency;
        this.status = MerchantStatus.ACTIVE;
    }

    // Business methods
    public boolean isActive() {
        return status == MerchantStatus.ACTIVE;
    }

    public boolean requiresCurrencyConversion(com.paymentprovider.paymentservice.domain.Currency paymentCurrency) {
        return autoCurrencyConversion && !settlementCurrency.equals(paymentCurrency);
    }

    // Getters and setters
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public MerchantStatus getStatus() { return status; }
    public void setStatus(MerchantStatus status) { this.status = status; }

    public com.paymentprovider.paymentservice.domain.Currency getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(com.paymentprovider.paymentservice.domain.Currency settlementCurrency) { this.settlementCurrency = settlementCurrency; }

    public Boolean getAutoCurrencyConversion() { return autoCurrencyConversion; }
    public void setAutoCurrencyConversion(Boolean autoCurrencyConversion) { this.autoCurrencyConversion = autoCurrencyConversion; }

    public List<PaymentMethodConfig> getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(List<PaymentMethodConfig> paymentMethods) { this.paymentMethods = paymentMethods; }

    public RiskProfile getRiskProfile() { return riskProfile; }
    public void setRiskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; }

    public SettlementConfig getSettlementConfig() { return settlementConfig; }
    public void setSettlementConfig(SettlementConfig settlementConfig) { this.settlementConfig = settlementConfig; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
