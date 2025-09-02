package com.paymentprovider.common.patterns.chain.components;

import com.paymentprovider.common.patterns.chain.PaymentContext;
import com.paymentprovider.common.patterns.chain.PaymentHandler;
import com.paymentprovider.common.patterns.chain.ProcessingResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Validation handler
 */
@Component
public class ValidationHandler extends PaymentHandler {

    @Override
    protected CompletableFuture<ProcessingResult> doHandle(PaymentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Validating payment: {}", context.getPaymentId());

            // Validate payment data
            if (context.getAmount() == null || context.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return ProcessingResult.failure("Invalid amount", getHandlerName());
            }

            if (context.getCurrency() == null || context.getCurrency().trim().isEmpty()) {
                return ProcessingResult.failure("Currency is required", getHandlerName());
            }

            if (context.getMerchantId() == null || context.getMerchantId().trim().isEmpty()) {
                return ProcessingResult.failure("Merchant ID is required", getHandlerName());
            }

            logger.info("Payment validation passed: {}", context.getPaymentId());
            return ProcessingResult.success("Validation passed", getHandlerName());
        });
    }

    @Override
    protected String getHandlerName() {
        return "ValidationHandler";
    }
}

