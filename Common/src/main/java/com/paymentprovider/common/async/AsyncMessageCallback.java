package com.paymentprovider.common.async;

import org.springframework.kafka.support.SendResult;

/**
 * Callback interface for async message publishing.
 * Implements Interface Segregation Principle - focused interface for callbacks.
 */
public interface AsyncMessageCallback {
    void onSuccess(SendResult<String, Object> result);
    void onFailure(Throwable throwable);
}
