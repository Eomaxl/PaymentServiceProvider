package com.paymentprovider.common.patterns.observer;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Publisher in Observer pattern for payment events.
 * Implements async notification with thread safety.
 */
@Component
public class PaymentEventPublisher {

    private final List<PaymentEventObserver> observers = new CopyOnWriteArrayList<>();
    private final Executor notificationExecutor;

    public PaymentEventPublisher(Executor notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    public void addObserver(PaymentEventObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(PaymentEventObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all observers asynchronously
     */
    public CompletableFuture<Void> notifyPaymentProcessed(PaymentEvent event) {
        return CompletableFuture.runAsync(() ->
                        observers.forEach(observer -> observer.onPaymentProcessed(event)),
                notificationExecutor
        );
    }

    public CompletableFuture<Void> notifyPaymentFailed(PaymentEvent event) {
        return CompletableFuture.runAsync(() ->
                        observers.forEach(observer -> observer.onPaymentFailed(event)),
                notificationExecutor
        );
    }

    public CompletableFuture<Void> notifyPaymentCancelled(PaymentEvent event) {
        return CompletableFuture.runAsync(() ->
                        observers.forEach(observer -> observer.onPaymentCancelled(event)),
                notificationExecutor
        );
    }
}