package com.paymentprovider.common.patterns.chain.components;

import com.paymentprovider.common.patterns.chain.PaymentContext;
import com.paymentprovider.common.patterns.chain.PaymentHandler;
import com.paymentprovider.common.patterns.chain.ProcessingResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Risk assessment handler
 */
@Component
public class RiskAssessmentHandler extends PaymentHandler {

    @Override
    protected CompletableFuture<ProcessingResult> doHandle(PaymentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Assessing risk for payment: {}", context.getPaymentId());

            int riskScore = calculateRiskScore(context);
            context.setRiskScore(riskScore);

            if (riskScore > 80) {
                logger.warn("High risk transaction: {} with score: {}", context.getPaymentId(), riskScore);
                return ProcessingResult.failure("High risk transaction", getHandlerName());
            }

            logger.info("Risk assessment passed: {} with score: {}", context.getPaymentId(), riskScore);
            return ProcessingResult.success("Risk assessment passed", getHandlerName());
        });
    }

    @Override
    protected String getHandlerName() {
        return "RiskAssessmentHandler";
    }

    private int calculateRiskScore(PaymentContext context) {
        int score = 0;

        // Amount-based risk
        if (context.getAmount().compareTo(new java.math.BigDecimal("1000")) > 0) {
            score += 20;
        }

        // Currency-based risk
        if (!"USD".equals(context.getCurrency()) && !"EUR".equals(context.getCurrency())) {
            score += 10;
        }

        // Customer history (simulated)
        if (context.getCustomerEmail() != null && context.getCustomerEmail().endsWith(".temp")) {
            score += 30;
        }

        return Math.min(score, 100);
    }
}

