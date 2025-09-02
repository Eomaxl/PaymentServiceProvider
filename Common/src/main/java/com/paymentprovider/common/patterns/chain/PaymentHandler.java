package com.paymentprovider.common.patterns.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Chain of Responsibility pattern for payment processing.
 * Follows Chain of Responsibility Pattern, Single Responsibility Principle, and Open/Closed Principle.
 */
public abstract class PaymentHandler {

    protected static final Logger logger = LoggerFactory.getLogger(PaymentHandler.class);

    protected PaymentHandler nextHandler;

    public PaymentHandler setNext(PaymentHandler handler) {
        this.nextHandler = handler;
        return handler;
    }

    public CompletableFuture<ProcessingResult> handle(PaymentContext context) {
        return doHandle(context).thenCompose(result -> {
            if (result.shouldContinue() && nextHandler != null) {
                return nextHandler.handle(context);
            }
            return CompletableFuture.completedFuture(result);
        });
    }

    protected abstract CompletableFuture<ProcessingResult> doHandle(PaymentContext context);
    protected abstract String getHandlerName();
}
