package com.paymentprovider.common.patterns.chain.components;

import com.paymentprovider.common.patterns.chain.PaymentContext;
import com.paymentprovider.common.patterns.chain.PaymentHandler;
import com.paymentprovider.common.patterns.chain.ProcessingResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Payment authorization handler
 */
@Component
public class AuthorizationHandler extends PaymentHandler {

    @Override
    protected CompletableFuture<ProcessingResult> doHandle(PaymentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Authorizing payment: {}", context.getPaymentId());

            // Simulate authorization logic
            if (context.getRiskScore() > 50) {
                logger.info("Requiring additional authorization for payment: {}", context.getPaymentId());
                context.setRequiresAdditionalAuth(true);
            }

            // Simulate authorization success
            String authCode = "AUTH_" + System.currentTimeMillis();
            context.setAuthorizationCode(authCode);

            logger.info("Payment authorized: {} with code: {}", context.getPaymentId(), authCode);
            return ProcessingResult.success("Payment authorized", getHandlerName());
        });
    }

    @Override
    protected String getHandlerName() {
        return "AuthorizationHandler";
    }
}

