package com.paymentprovider.paymentservice.event.consumer;

import com.paymentprovider.paymentservice.event.PaymentEvent;
import com.paymentprovider.paymentservice.event.PaymentEventType;
import com.paymentprovider.paymentservice.event.publisher.PaymentEventPublished;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Consumer for payment events published to the internal event bus.
 * Handles real-time processing of payment events for various business operations.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentEventHandler eventHandler;

    public PaymentEventConsumer(PaymentEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * Handle payment events asynchronously
     */
    @EventListener
    @Async
    public void handlePaymentEvent(PaymentEventPublished eventPublished) {
        PaymentEvent event = eventPublished.getPaymentEvent();

        try {
            logger.debug("Processing payment event: paymentId={}, eventType={}, eventId={}",
                    event.getPaymentId(), event.getEventType(), event.getEventId());

            // Route event to appropriate handler based on event type
            switch (event.getEventType()) {
                case PAYMENT_INITIATED:
                    eventHandler.handlePaymentInitiated(event);
                    break;
                case PAYMENT_AUTHORIZED:
                    eventHandler.handlePaymentAuthorized(event);
                    break;
                case PAYMENT_CAPTURED:
                    eventHandler.handlePaymentCaptured(event);
                    break;
                case PAYMENT_FAILED:
                    eventHandler.handlePaymentFailed(event);
                    break;
                case FRAUD_ALERT_TRIGGERED:
                    eventHandler.handleFraudAlert(event);
                    break;
                case WEBHOOK_FAILED:
                    eventHandler.handleWebhookFailed(event);
                    break;
                default:
                    eventHandler.handleGenericEvent(event);
                    break;
            }

            logger.debug("Successfully processed payment event: eventId={}", event.getEventId());

        } catch (Exception e) {
            logger.error("Failed to process payment event: eventId={}, paymentId={}, eventType={}",
                    event.getEventId(), event.getPaymentId(), event.getEventType(), e);

            // Handle event processing failure
            eventHandler.handleEventProcessingFailure(event, e);
        }
    }

    /**
     * Handle fraud-related events with higher priority
     */
    @EventListener(condition = "#eventPublished.paymentEvent.eventType.fraudRelated")
    @Async("fraudEventExecutor")
    public void handleFraudEvent(PaymentEventPublished eventPublished) {
        PaymentEvent event = eventPublished.getPaymentEvent();

        logger.warn("Processing fraud-related event: paymentId={}, eventType={}",
                event.getPaymentId(), event.getEventType());

        try {
            eventHandler.handleFraudRelatedEvent(event);
        } catch (Exception e) {
            logger.error("Failed to process fraud event: eventId={}, paymentId={}",
                    event.getEventId(), event.getPaymentId(), e);
            eventHandler.handleCriticalEventFailure(event, e);
        }
    }

    /**
     * Handle terminal payment events (completed, failed, cancelled)
     */
    @EventListener(condition = "#eventPublished.paymentEvent.eventType.terminal")
    public void handleTerminalEvent(PaymentEventPublished eventPublished) {
        PaymentEvent event = eventPublished.getPaymentEvent();

        logger.info("Processing terminal payment event: paymentId={}, eventType={}",
                event.getPaymentId(), event.getEventType());

        try {
            eventHandler.handleTerminalEvent(event);
        } catch (Exception e) {
            logger.error("Failed to process terminal event: eventId={}, paymentId={}",
                    event.getEventId(), event.getPaymentId(), e);
        }
    }
}
