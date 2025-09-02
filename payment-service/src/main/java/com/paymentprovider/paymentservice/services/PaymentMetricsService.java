package com.paymentprovider.paymentservice.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and managing payment-related metrics
 */
@Service
public class PaymentMetricsService {

    private final Counter paymentSuccessCounter;
    private final Counter paymentFailureCounter;
    private final Counter paymentVolumeCounter;
    private final Timer paymentProcessingTimer;
    private final Counter fraudDetectionCounter;
    private final Timer fraudAnalysisTimer;
    private final Counter routingDecisionCounter;
    private final Counter processorFailoverCounter;
    private final Counter currencyConversionCounter;
    private final Timer apiResponseTimer;
    private final MeterRegistry meterRegistry;

    // Gauges for real-time metrics
    private final AtomicLong activePayments = new AtomicLong(0);
    private final AtomicLong queuedPayments = new AtomicLong(0);
    private final AtomicLong dailyVolume = new AtomicLong(0);

    public PaymentMetricsService(
            Counter paymentSuccessCounter,
            Counter paymentFailureCounter,
            Counter paymentVolumeCounter,
            Timer paymentProcessingTimer,
            Counter fraudDetectionCounter,
            Timer fraudAnalysisTimer,
            Counter routingDecisionCounter,
            Counter processorFailoverCounter,
            Counter currencyConversionCounter,
            Timer apiResponseTimer,
            MeterRegistry meterRegistry) {

        this.paymentSuccessCounter = paymentSuccessCounter;
        this.paymentFailureCounter = paymentFailureCounter;
        this.paymentVolumeCounter = paymentVolumeCounter;
        this.paymentProcessingTimer = paymentProcessingTimer;
        this.fraudDetectionCounter = fraudDetectionCounter;
        this.fraudAnalysisTimer = fraudAnalysisTimer;
        this.routingDecisionCounter = routingDecisionCounter;
        this.processorFailoverCounter = processorFailoverCounter;
        this.currencyConversionCounter = currencyConversionCounter;
        this.apiResponseTimer = apiResponseTimer;
        this.meterRegistry = meterRegistry;

        // Register gauges
        Gauge.builder("payment.active.count", activePayments, AtomicLong::doubleValue)
                .description("Number of currently active payments")
                .register(meterRegistry);

        Gauge.builder("payment.queued.count", queuedPayments, AtomicLong::doubleValue)
                .description("Number of queued payments")
                .register(meterRegistry);

        Gauge.builder("payment.daily.volume", dailyVolume, AtomicLong::doubleValue)
                .description("Daily payment volume")
                .register(meterRegistry);
    }

    /**
     * Record a successful payment
     */
    public void recordPaymentSuccess(String paymentMethod, String currency, BigDecimal amount) {
        Counter.builder("payment.success.total")
                .tag("payment_method", paymentMethod)
                .tag("currency", currency)
                .register(meterRegistry)
                .increment();
        paymentVolumeCounter.increment(amount.doubleValue());
    }

    /**
     * Record a failed payment
     */
    public void recordPaymentFailure(String paymentMethod, String currency, String errorCode) {
        Counter.builder("payment.failure.total")
                .tag("payment_method", paymentMethod)
                .tag("currency", currency)
                .tag("error_code", errorCode)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record payment processing time
     */
    public Timer.Sample startPaymentTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop payment processing timer
     */
    public void stopPaymentTimer(Timer.Sample sample, String paymentMethod) {
        sample.stop(Timer.builder("payment.processing.duration")
                .tag("payment_method", paymentMethod)
                .register(meterRegistry));
    }

    /**
     * Record fraud detection
     */
    public void recordFraudDetection(String riskLevel, String reason) {
        Counter.builder("fraud.detection.total")
                .tag("risk_level", riskLevel)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record fraud analysis time
     */
    public Timer.Sample startFraudAnalysisTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop fraud analysis timer
     */
    public void stopFraudAnalysisTimer(Timer.Sample sample) {
        sample.stop(fraudAnalysisTimer);
    }

    /**
     * Record routing decision
     */
    public void recordRoutingDecision(String processor, String reason) {
        Counter.builder("routing.decision.total")
                .tag("processor", processor)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record processor failover
     */
    public void recordProcessorFailover(String fromProcessor, String toProcessor, String reason) {
        Counter.builder("processor.failover.total")
                .tag("from_processor", fromProcessor)
                .tag("to_processor", toProcessor)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record currency conversion
     */
    public void recordCurrencyConversion(String fromCurrency, String toCurrency, BigDecimal amount) {
        Counter.builder("currency.conversion.total")
                .tag("from_currency", fromCurrency)
                .tag("to_currency", toCurrency)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record API response time
     */
    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop API response timer
     */
    public void stopApiTimer(Timer.Sample sample, String endpoint, String method, int statusCode) {
        sample.stop(Timer.builder("api.response.duration")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status_code", String.valueOf(statusCode))
                .register(meterRegistry));
    }

    /**
     * Update active payments count
     */
    public void incrementActivePayments() {
        activePayments.incrementAndGet();
    }

    /**
     * Decrement active payments count
     */
    public void decrementActivePayments() {
        activePayments.decrementAndGet();
    }

    /**
     * Update queued payments count
     */
    public void setQueuedPayments(long count) {
        queuedPayments.set(count);
    }

    /**
     * Update daily volume
     */
    public void addToDailyVolume(BigDecimal amount) {
        dailyVolume.addAndGet(amount.longValue());
    }

    /**
     * Reset daily volume (called at midnight)
     */
    public void resetDailyVolume() {
        dailyVolume.set(0);
    }

    /**
     * Calculate payment success rate
     */
    public double getPaymentSuccessRate() {
        double successCount = paymentSuccessCounter.count();
        double failureCount = paymentFailureCounter.count();
        double totalCount = successCount + failureCount;

        if (totalCount == 0) {
            return 0.0;
        }

        return (successCount / totalCount) * 100.0;
    }
}