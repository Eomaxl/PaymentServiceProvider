package com.paymentprovider.paymentservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Payment response entity representing the response to a payment request
 */
public class PaymentResponse {

    private String paymentId;
    private String merchantId;
    private BigDecimal amount;
    private Currency currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String description;
    private String customerId;
    private String customerEmail;
    private String processorReference;
    private String processorName;
    private String authorizationCode;
    private String failureReason;
    private String failureCode;
    private String returnUrl;
    private Map<String, String> metadata;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Additional response-specific fields
    private String redirectUrl;
    private boolean requiresAction;
    private String actionType;
    private Map<String, Object> actionData;

    // Constructors
    public PaymentResponse() {}

    public PaymentResponse(Payment payment) {
        this.paymentId = payment.getPaymentId();
        this.merchantId = payment.getMerchantId();
        this.amount = payment.getAmount();
        this.currency = payment.getCurrency();
        this.paymentMethod = payment.getPaymentMethod();
        this.status = payment.getStatus();
        this.description = payment.getDescription();
        this.customerId = payment.getCustomerId();
        this.customerEmail = payment.getCustomerEmail();
        this.processorReference = payment.getProcessorReference();
        this.processorName = payment.getProcessorName();
        this.authorizationCode = payment.getAuthorizationCode();
        this.failureReason = payment.getFailureReason();
        this.failureCode = payment.getFailureCode();
        this.returnUrl = payment.getReturnUrl();
        this.metadata = payment.getMetadata();
        this.expiresAt = payment.getExpiresAt();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getUpdatedAt();
    }

    // Factory methods for different response types
    public static PaymentResponse success(Payment payment) {
        PaymentResponse response = new PaymentResponse(payment);
        response.requiresAction = false;
        return response;
    }

    public static PaymentResponse requiresAction(Payment payment, String actionType, String redirectUrl) {
        PaymentResponse response = new PaymentResponse(payment);
        response.requiresAction = true;
        response.actionType = actionType;
        response.redirectUrl = redirectUrl;
        return response;
    }

    public static PaymentResponse requiresAction(Payment payment, String actionType, Map<String, Object> actionData) {
        PaymentResponse response = new PaymentResponse(payment);
        response.requiresAction = true;
        response.actionType = actionType;
        response.actionData = actionData;
        return response;
    }

    public static PaymentResponse failed(Payment payment) {
        return new PaymentResponse(payment);
    }

    public static PaymentResponse fromPayment(Payment payment) {
        return new PaymentResponse(payment);
    }

    // Business methods
    public boolean isSuccessful() {
        return status != null && status.isSuccessful();
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.AUTHORIZED;
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

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public boolean isRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(boolean requiresAction) {
        this.requiresAction = requiresAction;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getActionData() {
        return actionData;
    }

    public void setActionData(Map<String, Object> actionData) {
        this.actionData = actionData;
    }
}
