package com.paymentprovider.common.patterns.observer;

/**
 * Observer pattern interface for payment events.
 * Follows Interface Segregation Principle.
 */
public interface PaymentEventObserver {
    void onPaymentProcessed(PaymentEvent event);
    void onPaymentFailed(PaymentEvent event);
    void onPaymentCancelled(PaymentEvent event);
}