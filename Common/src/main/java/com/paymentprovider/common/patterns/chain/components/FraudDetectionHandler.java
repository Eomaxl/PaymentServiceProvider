package com.paymentprovider.common.patterns.chain.components;

import com.paymentprovider.common.patterns.chain.PaymentContext;
import com.paymentprovider.common.patterns.chain.PaymentHandler;
import com.paymentprovider.common.patterns.chain.ProcessingResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Fraud detection handler
 */
@Component
public class FraudDetectionHandler extends PaymentHandler {

    @Override
    protected CompletableFuture<ProcessingResult> doHandle(PaymentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Performing fraud check for payment: {}", context.getPaymentId());

            // Simulate fraud detection logic
            if (context.getAmount().compareTo(new java.math.BigDecimal("10000")) > 0) {
                logger.warn("High amount transaction flagged for review: {}", context.getPaymentId());
                return ProcessingResult.failure("Transaction flagged for manual review", getHandlerName());
            }

            // Check for suspicious patterns
            if (context.getCustomerEmail() != null && context.getCustomerEmail().contains("suspicious")) {
                logger.warn("Suspicious customer email detected: {}", context.getPaymentId());
                return ProcessingResult.failure("Suspicious activity detected", getHandlerName());
            }

            logger.info("Fraud check passed: {}", context.getPaymentId());
            return ProcessingResult.success("Fraud check passed", getHandlerName());
        });
    }

    @Override
    protected String getHandlerName() {
        return "FraudDetectionHandler";
    }
}

