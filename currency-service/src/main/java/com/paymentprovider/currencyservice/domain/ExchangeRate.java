package com.paymentprovider.currencyservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency", "rate_date"}))
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 3)
    @NotNull
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    @NotNull
    private String toCurrency;

    @Column(nullable = false, precision = 19, scale = 8)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal rate;

    @Column(name = "rate_date", nullable = false)
    @NotNull
    private Instant rateDate;

    @Column(name = "source_provider")
    private String sourceProvider;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Constructors
    public ExchangeRate() {}

    public ExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, Instant rateDate) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
