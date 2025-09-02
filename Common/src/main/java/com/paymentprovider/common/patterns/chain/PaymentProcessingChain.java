package com.paymentprovider.common.patterns.chain;

import com.paymentprovider.common.patterns.chain.components.*;
import org.springframework.stereotype.Component;

/**
 * Chain builder for creating payment processing chains
 */
@Component
public class PaymentProcessingChainBuilder {

    private final ValidationHandler validationHandler;
    private final FraudDetectionHandler fraudDetectionHandler;
    private final RiskAssessmentHandler riskAssessmentHandler;
    private final AuthorizationHandler authorizationHandler;
    private final PaymentProcessingHandler paymentProcessingHandler;

    public PaymentProcessingChainBuilder(ValidationHandler validationHandler,
                                         FraudDetectionHandler fraudDetectionHandler,
                                         RiskAssessmentHandler riskAssessmentHandler,
                                         AuthorizationHandler authorizationHandler,
                                         PaymentProcessingHandler paymentProcessingHandler) {
        this.validationHandler = validationHandler;
        this.fraudDetectionHandler = fraudDetectionHandler;
        this.riskAssessmentHandler = riskAssessmentHandler;
        this.authorizationHandler = authorizationHandler;
        this.paymentProcessingHandler = paymentProcessingHandler;
    }

    /**
     * Build standard payment processing chain
     */
    public PaymentHandler buildStandardChain() {
        validationHandler
                .setNext(fraudDetectionHandler)
                .setNext(riskAssessmentHandler)
                .setNext(authorizationHandler)
                .setNext(paymentProcessingHandler);

        return validationHandler;
    }

    /**
     * Build express payment processing chain (skip some checks)
     */
    public PaymentHandler buildExpressChain() {
        validationHandler
                .setNext(authorizationHandler)
                .setNext(paymentProcessingHandler);

        return validationHandler;
    }

    /**
     * Build high-security payment processing chain
     */
    public PaymentHandler buildHighSecurityChain() {
        validationHandler
                .setNext(fraudDetectionHandler)
                .setNext(riskAssessmentHandler)
                .setNext(authorizationHandler)
                .setNext(paymentProcessingHandler);

        return validationHandler;
    }
}

/**
 * Processing result
 */
class ProcessingResult {
    private final boolean success;
    private final String message;
    private final String handlerName;
    private final boolean shouldContinue;

    private ProcessingResult(boolean success, String message, String handlerName, boolean shouldContinue) {
        this.success = success;
        this.message = message;
        this.handlerName = handlerName;
        this.shouldContinue = shouldContinue;
    }

    public static ProcessingResult success(String message, String handlerName) {
        return new ProcessingResult(true, message, handlerName, true);
    }

    public static ProcessingResult success(String message, String handlerName, boolean shouldContinue) {
        return new ProcessingResult(true, message, handlerName, shouldContinue);
    }

    public static ProcessingResult failure(String message, String handlerName) {
        return new ProcessingResult(false, message, handlerName, false);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getHandlerName() { return handlerName; }
    public boolean shouldContinue() { return shouldContinue; }
}

/**
 * Payment context that flows through the chain
 */
class PaymentContext {
    private String paymentId;
    private java.math.BigDecimal amount;
    private String currency;
    private String merchantId;
    private String customerEmail;
    private String status;
    private String transactionId;
    private String authorizationCode;
    private int riskScore;
    private boolean requiresAdditionalAuth;

    // Constructors, getters, and setters
    public PaymentContext(String paymentId, java.math.BigDecimal amount, String currency, String merchantId) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
    }

    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public java.math.BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMerchantId() { return merchantId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public boolean isRequiresAdditionalAuth() { return requiresAdditionalAuth; }
    public void setRequiresAdditionalAuth(boolean requiresAdditionalAuth) { this.requiresAdditionalAuth = requiresAdditionalAuth; }
}
