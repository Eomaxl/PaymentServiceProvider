package com.paymentprovider.common.patterns.chain.components;

import com.paymentprovider.common.patterns.chain.PaymentContext;
import com.paymentprovider.common.patterns.chain.PaymentHandler;
import com.paymentprovider.common.patterns.chain.ProcessingResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Payment processing handler (final handler)
 */
@Component
public class PaymentProcessingHandler extends PaymentHandler {

    @Override
    protected CompletableFuture<ProcessingResult> doHandle(PaymentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Processing payment: {}", context.getPaymentId());

            // Simulate payment processing
            String transactionId = "TXN_" + System.currentTimeMillis();
            context.setTransactionId(transactionId);
            context.setStatus("COMPLETED");

            logger.info("Payment processed successfully: {} with transaction: {}",
                    context.getPaymentId(), transactionId);

            return ProcessingResult.success("Payment processed successfully", getHandlerName(), false);
        });
    }

    @Override
    protected String getHandlerName() {
        return "PaymentProcessingHandler";
    }
}

