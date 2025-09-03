//package com.paymentprovider.common.patterns.observer;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.Executor;
//
///**
// * Observer pattern implementation for event handling.
// * Follows Observer Pattern, Single Responsibility Principle, and Open/Closed Principle.
// */
//public interface EventObserver<T> {
//    void onEvent(T event);
//    String getObserverName();
//    boolean canHandle(T event);
//}
//
///**
// * Event subject that notifies observers
// */
//@Component
//class EventSubject<T> {
//
//    private static final Logger logger = LoggerFactory.getLogger(EventSubject.class);
//
//    private final ConcurrentHashMap<Class<?>, List<EventObserver<T>>> observers = new ConcurrentHashMap<>();
//    private final Executor notificationExecutor;
//
//    public EventSubject(Executor notificationExecutor) {
//        this.notificationExecutor = notificationExecutor;
//    }
//
//    /**
//     * Register an observer for specific event type
//     */
//    public void registerObserver(Class<?> eventType, EventObserver<T> observer) {
//        observers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(observer);
//        logger.debug("Registered observer {} for event type {}", observer.getObserverName(), eventType.getSimpleName());
//    }
//
//    /**
//     * Unregister an observer
//     */
//    public void unregisterObserver(Class<?> eventType, EventObserver<T> observer) {
//        List<EventObserver<T>> eventObservers = observers.get(eventType);
//        if (eventObservers != null) {
//            eventObservers.remove(observer);
//            logger.debug("Unregistered observer {} for event type {}", observer.getObserverName(), eventType.getSimpleName());
//        }
//    }
//
//    /**
//     * Notify all observers asynchronously
//     */
//    public CompletableFuture<Void> notifyObserversAsync(T event) {
//        Class<?> eventType = event.getClass();
//        List<EventObserver<T>> eventObservers = observers.get(eventType);
//
//        if (eventObservers == null || eventObservers.isEmpty()) {
//            return CompletableFuture.completedFuture(null);
//        }
//
//        List<CompletableFuture<Void>> futures = eventObservers.stream()
//                .filter(observer -> observer.canHandle(event))
//                .map(observer -> CompletableFuture.runAsync(() -> {
//                    try {
//                        observer.onEvent(event);
//                        logger.debug("Observer {} handled event {}", observer.getObserverName(), eventType.getSimpleName());
//                    } catch (Exception e) {
//                        logger.error("Observer {} failed to handle event {}", observer.getObserverName(), eventType.getSimpleName(), e);
//                    }
//                }, notificationExecutor))
//                .toList();
//
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//    }
//
//    /**
//     * Notify observers synchronously
//     */
//    public void notifyObservers(T event) {
//        notifyObserversAsync(event).join();
//    }
//
//    /**
//     * Get observer count for event type
//     */
//    public int getObserverCount(Class<?> eventType) {
//        List<EventObserver<T>> eventObservers = observers.get(eventType);
//        return eventObservers != null ? eventObservers.size() : 0;
//    }
//}
//
///**
// * Payment event observer
// */
//@Component
//public class PaymentEventObserver implements EventObserver<PaymentEvent> {
//
//    private static final Logger logger = LoggerFactory.getLogger(PaymentEventObserver.class);
//
//    @Override
//    public void onEvent(PaymentEvent event) {
//        logger.info("Processing payment event: {} for payment: {}", event.getEventType(), event.getPaymentId());
//
//        switch (event.getEventType()) {
//            case "PAYMENT_INITIATED":
//                handlePaymentInitiated(event);
//                break;
//            case "PAYMENT_COMPLETED":
//                handlePaymentCompleted(event);
//                break;
//            case "PAYMENT_FAILED":
//                handlePaymentFailed(event);
//                break;
//            default:
//                logger.warn("Unknown payment event type: {}", event.getEventType());
//        }
//    }
//
//    @Override
//    public String getObserverName() {
//        return "PaymentEventObserver";
//    }
//
//    @Override
//    public boolean canHandle(PaymentEvent event) {
//        return event.getEventType().startsWith("PAYMENT_");
//    }
//
//    private void handlePaymentInitiated(PaymentEvent event) {
//        // Handle payment initiation logic
//        logger.debug("Handling payment initiated: {}", event.getPaymentId());
//    }
//
//    private void handlePaymentCompleted(PaymentEvent event) {
//        // Handle payment completion logic
//        logger.debug("Handling payment completed: {}", event.getPaymentId());
//    }
//
//    private void handlePaymentFailed(PaymentEvent event) {
//        // Handle payment failure logic
//        logger.debug("Handling payment failed: {}", event.getPaymentId());
//    }
//}
//
///**
// * Fraud event observer
// */
//@Component
//public class FraudEventObserver implements EventObserver<PaymentEvent> {
//
//    private static final Logger logger = LoggerFactory.getLogger(FraudEventObserver.class);
//
//    @Override
//    public void onEvent(PaymentEvent event) {
//        logger.info("Processing fraud event: {} for payment: {}", event.getEventType(), event.getPaymentId());
//
//        switch (event.getEventType()) {
//            case "FRAUD_DETECTED":
//                handleFraudDetected(event);
//                break;
//            case "FRAUD_CLEARED":
//                handleFraudCleared(event);
//                break;
//            default:
//                logger.warn("Unknown fraud event type: {}", event.getEventType());
//        }
//    }
//
//    @Override
//    public String getObserverName() {
//        return "FraudEventObserver";
//    }
//
//    @Override
//    public boolean canHandle(PaymentEvent event) {
//        return event.getEventType().startsWith("FRAUD_");
//    }
//
//    private void handleFraudDetected(PaymentEvent event) {
//        // Handle fraud detection logic
//        logger.debug("Handling fraud detected: {}", event.getPaymentId());
//    }
//
//    private void handleFraudCleared(PaymentEvent event) {
//        // Handle fraud cleared logic
//        logger.debug("Handling fraud cleared: {}", event.getPaymentId());
//    }
//}
//
///**
// * Simple payment event class for demonstration
// */
//class PaymentEvent {
//    private final String paymentId;
//    private final String eventType;
//    private final Object data;
//
//    public PaymentEvent(String paymentId, String eventType, Object data) {
//        this.paymentId = paymentId;
//        this.eventType = eventType;
//        this.data = data;
//    }
//
//    public String getPaymentId() { return paymentId; }
//    public String getEventType() { return eventType; }
//    public Object getData() { return data; }
//}