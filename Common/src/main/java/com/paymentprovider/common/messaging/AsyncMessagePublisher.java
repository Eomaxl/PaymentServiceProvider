package com.paymentprovider.common.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Async message publisher implementing Observer pattern for inter-service communication.
 * Follows Single Responsibility Principle - handles only message publishing.
 */
@Component
public class AsyncMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessagePublisher.class);

    private final ConcurrentHashMap<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();
    private final Executor messagingExecutor;

    public AsyncMessagePublisher(@Qualifier("messagingExecutor") Executor messagingExecutor) {
        this.messagingExecutor = messagingExecutor;
    }

    /**
     * Subscribe to messages of a specific type
     */
    public <T> void subscribe(String messageType, Consumer<T> handler) {
        subscribers.computeIfAbsent(messageType, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) handler);
        logger.debug("Subscribed to message type: {}", messageType);
    }

    /**
     * Publish message asynchronously to all subscribers
     */
    public <T> CompletableFuture<Void> publishAsync(String messageType, T message) {
        return CompletableFuture.runAsync(() -> {
            List<Consumer<Object>> handlers = subscribers.get(messageType);
            if (handlers != null && !handlers.isEmpty()) {
                logger.debug("Publishing message type: {} to {} subscribers", messageType, handlers.size());

                List<CompletableFuture<Void>> futures = handlers.stream()
                        .map(handler -> CompletableFuture.runAsync(() -> {
                            try {
                                handler.accept(message);
                            } catch (Exception e) {
                                logger.error("Error handling message type: {}", messageType, e);
                            }
                        }, messagingExecutor))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        }, messagingExecutor);
    }

    /**
     * Publish message synchronously
     */
    public <T> void publish(String messageType, T message) {
        publishAsync(messageType, message).join();
    }

    /**
     * Unsubscribe from messages
     */
    public void unsubscribe(String messageType, Consumer<Object> handler) {
        List<Consumer<Object>> handlers = subscribers.get(messageType);
        if (handlers != null) {
            handlers.remove(handler);
            logger.debug("Unsubscribed from message type: {}", messageType);
        }
    }

    /**
     * Get subscriber count for monitoring
     */
    public int getSubscriberCount(String messageType) {
        List<Consumer<Object>> handlers = subscribers.get(messageType);
        return handlers != null ? handlers.size() : 0;
    }
}