package com.paymentprovider.paymentservice.domain;

import com.paymentprovider.payment.security.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Payment instrument containing payment method specific data
 */
@Entity
@Table(name = "payment_instruments")
public class PaymentInstrument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    // Card-specific fields (encrypted)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "card_number", length = 500)
    @Size(min = 13, max = 19, message = "Card number must be between 13 and 19 digits")
    @Pattern(regexp = "^[0-9]+$", message = "Card number must contain only digits")
    private String cardNumber;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "cvv", length = 500)
    @Size(min = 3, max = 4, message = "CVV must be 3-4 digits")
    @Pattern(regexp = "^[0-9]+$", message = "CVV must contain only digits")
    private String cvv;

    @Column(name = "cardholder_name")
    @Size(max = 100, message = "Cardholder name must not exceed 100 characters")
    private String cardHolderName;

    // Digital wallet fields (encrypted)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "wallet_token", length = 500)
    @Size(max = 255, message = "Wallet token must not exceed 255 characters")
    private String walletToken;

    @Column(name = "wallet_type")
    @Size(max = 50, message = "Wallet type must not exceed 50 characters")
    private String walletType;

    // Bank transfer fields (encrypted)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "iban", length = 500)
    @Size(max = 34, message = "IBAN must not exceed 34 characters")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]+$", message = "IBAN format is invalid")
    private String iban;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "bic", length = 500)
    @Size(max = 11, message = "BIC must not exceed 11 characters")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "BIC format is invalid")
    private String bic;

    @Column(name = "account_holder_name")
    @Size(max = 100, message = "Account holder name must not exceed 100 characters")
    private String accountHolderName;

    // Generic token for stored payment methods (encrypted)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "payment_token", length = 500)
    @Size(max = 255, message = "Payment token must not exceed 255 characters")
    private String paymentToken;

    // Constructors
    public PaymentInstrument() {}

    // Factory methods for different payment types
    public static PaymentInstrument forCard(String cardNumber, Integer expiryMonth,
                                            Integer expiryYear, String cvv, String cardholderName) {
        PaymentInstrument instrument = new PaymentInstrument();
        instrument.paymentMethod = PaymentMethod.CREDIT_CARD;
        instrument.cardNumber = cardNumber;
        instrument.expiryMonth = expiryMonth;
        instrument.expiryYear = expiryYear;
        instrument.cvv = cvv;
        instrument.cardHolderName = cardholderName;
        return instrument;
    }

    public static PaymentInstrument forWallet(String walletToken, String walletType) {
        PaymentInstrument instrument = new PaymentInstrument();
        instrument.paymentMethod = PaymentMethod.DIGITAL_WALLET;
        instrument.walletToken = walletToken;
        instrument.walletType = walletType;
        return instrument;
    }

    public static PaymentInstrument forBankTransfer(String iban, String bic, String accountHolderName) {
        PaymentInstrument instrument = new PaymentInstrument();
        instrument.paymentMethod = PaymentMethod.BANK_TRANSFER;
        instrument.iban = iban;
        instrument.bic = bic;
        instrument.accountHolderName = accountHolderName;
        return instrument;
    }

    public static PaymentInstrument forToken(String paymentToken) {
        PaymentInstrument instrument = new PaymentInstrument();
        instrument.paymentMethod = PaymentMethod.STORED_PAYMENT;
        instrument.paymentToken = paymentToken;
        return instrument;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
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

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
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
