package com.paymentprovider.paymentservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Payment method configuration for merchants
 */
@Entity
@Table(name = "payment_method_configs")
public class PaymentMethodConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "min_amount", precision = 12, scale = 2)
    private java.math.BigDecimal minAmount;

    @Column(name = "max_amount", precision = 12, scale = 2)
    private java.math.BigDecimal maxAmount;

    // Constructors
    public PaymentMethodConfig() {}

    public PaymentMethodConfig(Merchant merchant, PaymentMethod paymentMethod) {
        this.merchant = merchant;
        this.paymentMethod = paymentMethod;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public java.math.BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(java.math.BigDecimal minAmount) { this.minAmount = minAmount; }

    public java.math.BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(java.math.BigDecimal maxAmount) { this.maxAmount = maxAmount; }
}
