package com.paymentprovider.common.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

/**
 * Async message publisher implementing Publisher pattern for event-driven communication.
 * Follows Single Responsibility Principle - only handles message publishing.
 */
@Component
public class AsyncMessagePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public AsyncMessagePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes message asynchronously using CompletableFuture
     */
    public CompletableFuture<SendResult<String, Object>> publishAsync(String topic, String key, Object message) {
        return kafkaTemplate.send(topic, key, message);
    }

    /**
     * Publishes message with callback handling
     */
    public void publishWithCallback(String topic, String key, Object message,
                                    AsyncMessageCallback callback) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                callback.onFailure(throwable);
            } else {
                callback.onSuccess(result);
            }
        });
    }
}
