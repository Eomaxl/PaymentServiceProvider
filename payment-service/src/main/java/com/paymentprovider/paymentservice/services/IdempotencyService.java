package com.paymentprovider.paymentservice.services;

import com.paymentprovider.payment.domain.Payment;
import com.paymentprovider.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service for handling payment idempotency
 */
@Service
public class IdempotencyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final PaymentRepository paymentRepository;

    @Autowired
    public IdempotencyService(RedisTemplate<String, String> redisTemplate, PaymentRepository paymentRepository) {
        this.redisTemplate = redisTemplate;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Stores the mapping between idempotency key and payment ID
     */
    public void storePaymentMapping(String idempotencyKey, String paymentId) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, paymentId, IDEMPOTENCY_TTL);
    }

    /**
     * Retrieves existing payment for the given idempotency key
     */
    public Optional<Payment> getExistingPayment(String idempotencyKey) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String paymentId = redisTemplate.opsForValue().get(key);

        if (paymentId != null) {
            return paymentRepository.findById(paymentId);
        }

        return Optional.empty();
    }

    /**
     * Removes idempotency key mapping
     */
    public void removeIdempotencyKey(String idempotencyKey) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.delete(key);
    }
}
