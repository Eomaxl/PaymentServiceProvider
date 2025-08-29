package com.paymentprovider.paymentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable class for storing payment instrument data in the Payment entity
 * This data will be encrypted when stored in the database
 */
@Embeddable
public class PaymentInstrumentData {

    @Column(name = "encrypted_card_number", length = 500)
    private String encryptedCardNumber;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "expiry_month", length = 2)
    private String expiryMonth;

    @Column(name = "expiry_year", length = 4)
    private String expiryYear;

    @Column(name = "encrypted_cvv", length = 500)
    private String encryptedCvv;

    @Column(name = "cardholder_name", length = 100)
    private String cardholderName;

    @Column(name = "wallet_token", length = 255)
    private String walletToken;

    @Column(name = "wallet_type", length = 50)
    private String walletType;

    @Column(name = "encrypted_iban", length = 500)
    private String encryptedIban;

    @Column(name = "bic", length = 11)
    private String bic;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "payment_token", length = 255)
    private String paymentToken;

    // Constructors
    public PaymentInstrumentData() {}

    /**
     * Factory method to create PaymentInstrumentData from PaymentInstrument
     */
    public static PaymentInstrumentData fromPaymentInstrument(PaymentInstrument paymentInstrument) {
        PaymentInstrumentData data = new PaymentInstrumentData();

        // Copy card data
        data.setCardLastFour(paymentInstrument.getCardNumber() != null && paymentInstrument.getCardNumber().length() >= 4
                ? paymentInstrument.getCardNumber().substring(paymentInstrument.getCardNumber().length() - 4)
                : null);
        data.setExpiryMonth(paymentInstrument.getExpiryMonth() != null ? String.format("%02d", paymentInstrument.getExpiryMonth()) : null);
        data.setExpiryYear(paymentInstrument.getExpiryYear() != null ? paymentInstrument.getExpiryYear().toString() : null);
        data.setCardholderName(paymentInstrument.getCardHolderName());

        // Copy wallet data
        data.setWalletToken(paymentInstrument.getWalletToken());
        data.setWalletType(paymentInstrument.getWalletType());

        // Copy bank account data
        data.setBic(paymentInstrument.getBic());
        data.setAccountHolderName(paymentInstrument.getAccountHolderName());

        // Copy payment token
        data.setPaymentToken(paymentInstrument.getPaymentToken());

        // Note: Encrypted fields (card number, CVV, IBAN) should be set separately by encryption service

        return data;
    }

    // Getters and setters
    public String getEncryptedCardNumber() {
        return encryptedCardNumber;
    }

    public void setEncryptedCardNumber(String encryptedCardNumber) {
        this.encryptedCardNumber = encryptedCardNumber;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getEncryptedCvv() {
        return encryptedCvv;
    }

    public void setEncryptedCvv(String encryptedCvv) {
        this.encryptedCvv = encryptedCvv;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getWalletToken() {
        return walletToken;
    }

    public void setWalletToken(String walletToken) {
        this.walletToken = walletToken;
    }

    public String getWalletType() {
        return walletType;
    }

    public void setWalletType(String walletType) {
        this.walletType = walletType;
    }

    public String getEncryptedIban() {
        return encryptedIban;
    }

    public void setEncryptedIban(String encryptedIban) {
        this.encryptedIban = encryptedIban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }
}
