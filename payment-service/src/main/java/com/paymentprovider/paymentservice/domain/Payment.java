package com.paymentprovider.paymentservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment entity representing a payment transaction
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_created_at", columnList = "created_at"),
        @Index(name = "idx_payment_customer_id", columnList = "customer_id")
})
public class Payment {

    @Id
    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @NotBlank(message = "Merchant ID is required")
    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Email(message = "Customer email must be valid")
    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "processor_reference", length = 100)
    private String processorReference;

    @Column(name = "processor_name", length = 50)
    private String processorName;

    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "return_url", length = 255)
    private String returnUrl;

    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @ElementCollection
    @CollectionTable(name = "payment_metadata", joinColumns = @JoinColumn(name = "payment_id"))
    @MapKeyColumn(name = "metadata_key", length = 100)
    @Column(name = "metadata_value", length = 500)
    private Map<String, String> metadata;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Embedded payment instrument data (encrypted)
    @Embedded
    private PaymentInstrumentData paymentInstrumentData;

    @Embedded
    private BillingAddressData billingAddressData;

    // Constructors
    public Payment() {
        this.paymentId = UUID.randomUUID().toString();
        this.status = PaymentStatus.PENDING;
    }

    public Payment(String merchantId, BigDecimal amount, Currency currency, PaymentMethod paymentMethod) {
        this();
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
    }

    // Business methods
    public boolean isTerminal() {
        return status.isTerminal();
    }

    public boolean isSuccessful() {
        return status.isSuccessful();
    }

    public boolean canBeAuthorized() {
        return status == PaymentStatus.PENDING;
    }

    public boolean canBeCaptured() {
        return status == PaymentStatus.AUTHORIZED;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.CAPTURED || status == PaymentStatus.SETTLED;
    }

    public void authorize(String authorizationCode, String processorReference) {
        if (!canBeAuthorized()) {
            throw new IllegalStateException("Payment cannot be authorized in current status: " + status);
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizationCode = authorizationCode;
        this.processorReference = processorReference;
    }

    public void capture() {
        if (!canBeCaptured()) {
            throw new IllegalStateException("Payment cannot be captured in current status: " + status);
        }
        this.status = PaymentStatus.CAPTURED;
    }

    public void fail(String failureReason, String failureCode) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.failureCode = failureCode;
    }

    public void decline(String failureReason, String failureCode) {
        this.status = PaymentStatus.DECLINED;
        this.failureReason = failureReason;
        this.failureCode = failureCode;
    }

    // Getters and setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getProcessorReference() {
        return processorReference;
    }

    public void setProcessorReference(String processorReference) {
        this.processorReference = processorReference;
    }

    public String getProcessorName() {
        return processorName;
    }

    public void setProcessorName(String processorName) {
        this.processorName = processorName;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public PaymentInstrumentData getPaymentInstrumentData() {
        return paymentInstrumentData;
    }

    public void setPaymentInstrumentData(PaymentInstrumentData paymentInstrumentData) {
        this.paymentInstrumentData = paymentInstrumentData;
    }

    public BillingAddressData getBillingAddressData() {
        return billingAddressData;
    }

    public void setBillingAddressData(BillingAddressData billingAddressData) {
        this.billingAddressData = billingAddressData;
    }
}
