package com.paymentprovider.paymentservice.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

/**
 * Settlement configuration for merchants
 */
@Embeddable
public class SettlementConfig {

    @NotNull
    @Enumerated(EnumType.STRING)
    private Currency settlementCurrency;

    private String settlementAccount;

    private Integer settlementDelayDays;

    // Constructors
    public SettlementConfig() {}

    public SettlementConfig(Currency settlementCurrency, String settlementAccount, Integer settlementDelayDays) {
        this.settlementCurrency = settlementCurrency;
        this.settlementAccount = settlementAccount;
        this.settlementDelayDays = settlementDelayDays;
    }

    // Getters and setters
    public Currency getSettlementCurrency() {
        return settlementCurrency;
    }

    public void setSettlementCurrency(Currency settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }

    public String getSettlementAccount() {
        return settlementAccount;
    }

    public void setSettlementAccount(String settlementAccount) {
        this.settlementAccount = settlementAccount;
    }

    public Integer getSettlementDelayDays() {
        return settlementDelayDays;
    }

    public void setSettlementDelayDays(Integer settlementDelayDays) {
        this.settlementDelayDays = settlementDelayDays;
    }
}
