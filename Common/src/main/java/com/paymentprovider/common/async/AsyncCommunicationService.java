package com.paymentprovider.common.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive async communication service implementing multiple patterns.
 * Follows Single Responsibility Principle and Dependency Inversion Principle.
 */
@Service
public class AsyncCommunicationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient webClient;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> replyFutures = new ConcurrentHashMap<>();

    @Autowired
    public AsyncCommunicationService(KafkaTemplate<String, Object> kafkaTemplate,
                                     WebClient.Builder webClientBuilder) {
        this.kafkaTemplate = kafkaTemplate;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Async HTTP communication using WebClient
     */
    public CompletableFuture<String> sendAsyncHttpRequest(String url, Object payload) {
        return webClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }

    /**
     * Reactive HTTP communication
     */
    public Mono<String> sendReactiveHttpRequest(String url, Object payload) {
        return webClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Async message publishing with callback
     */
    public CompletableFuture<Void> publishMessageAsync(String topic, String key, Object message) {
        return kafkaTemplate.send(topic, key, message)
                .thenApply(result -> null);
    }

    /**
     * Request-Reply pattern implementation
     */
    public CompletableFuture<Object> sendRequestReply(String requestTopic,
                                                      String replyTopic,
                                                      String correlationId,
                                                      Object request) {
        // Store correlation ID for reply matching
        CompletableFuture<Object> replyFuture = new CompletableFuture<>();
        replyFutures.put(correlationId, replyFuture);

        // Send request
        RequestMessage requestMessage = new RequestMessage(correlationId, replyTopic, request);
        kafkaTemplate.send(requestTopic, correlationId, requestMessage);

        return replyFuture;
    }

    /**
     * Handle reply messages
     */
    @KafkaListener(topics = "payment-replies,fraud-replies,routing-replies")
    public void handleReply(ReplyMessage reply) {
        CompletableFuture<Object> future = replyFutures.remove(reply.getCorrelationId());
        if (future != null) {
            future.complete(reply.getPayload());
        }
    }

    /**
     * Saga pattern implementation for distributed transactions
     */
    public CompletableFuture<SagaResult> executeSaga(SagaDefinition saga) {
        return CompletableFuture.supplyAsync(() -> {
            SagaResult result = new SagaResult(saga.getSagaId());
            for (SagaStep step : saga.getSteps()) {
                try {
                    Object stepResult = executeStep(step).join();
                    result.addStepResult(step.getStepId(), stepResult);
                } catch (Exception e) {
                    // Execute compensation
                    executeCompensation(saga, result);
                    result.setFailed(true);
                    result.setError(e.getMessage());
                    break;
                }
            }
            return result;
        });
    }

    private CompletableFuture<String> executeStep(SagaStep step) {
        return sendAsyncHttpRequest(step.getServiceUrl(), step.getPayload());
    }

    private void executeCompensation(SagaDefinition saga, SagaResult result) {
        // Execute compensation actions in reverse order
        for (int i = result.getCompletedSteps().size() - 1; i >= 0; i--) {
            SagaStep step = saga.getSteps().get(i);
            if (step.getCompensationAction() != null) {
                try {
                    sendAsyncHttpRequest(step.getCompensationAction().getServiceUrl(),
                            step.getCompensationAction().getPayload()).join();
                } catch (Exception e) {
                    // Log compensation failure
                }
            }
        }
    }

    // Helper classes
    public static class RequestMessage {
        private final String correlationId;
        private final String replyTopic;
        private final Object payload;

        public RequestMessage(String correlationId, String replyTopic, Object payload) {
            this.correlationId = correlationId;
            this.replyTopic = replyTopic;
            this.payload = payload;
        }

        // Getters
        public String getCorrelationId() { return correlationId; }
        public String getReplyTopic() { return replyTopic; }
        public Object getPayload() { return payload; }
    }

    public static class ReplyMessage {
        private final String correlationId;
        private final Object payload;

        public ReplyMessage(String correlationId, Object payload) {
            this.correlationId = correlationId;
            this.payload = payload;
        }

        // Getters
        public String getCorrelationId() { return correlationId; }
        public Object getPayload() { return payload; }
    }

    public static class SagaDefinition {
        private final String sagaId;
        private final java.util.List<SagaStep> steps;

        public SagaDefinition(String sagaId, java.util.List<SagaStep> steps) {
            this.sagaId = sagaId;
            this.steps = steps;
        }

        // Getters
        public String getSagaId() { return sagaId; }
        public java.util.List<SagaStep> getSteps() { return steps; }
    }

    public static class SagaStep {
        private final String stepId;
        private final String serviceUrl;
        private final Object payload;
        private final CompensationAction compensationAction;

        public SagaStep(String stepId, String serviceUrl, Object payload, CompensationAction compensationAction) {
            this.stepId = stepId;
            this.serviceUrl = serviceUrl;
            this.payload = payload;
            this.compensationAction = compensationAction;
        }

        // Getters
        public String getStepId() { return stepId; }
        public String getServiceUrl() { return serviceUrl; }
        public Object getPayload() { return payload; }
        public CompensationAction getCompensationAction() { return compensationAction; }
    }

    public static class CompensationAction {
        private final String serviceUrl;
        private final Object payload;

        public CompensationAction(String serviceUrl, Object payload) {
            this.serviceUrl = serviceUrl;
            this.payload = payload;
        }

        // Getters
        public String getServiceUrl() { return serviceUrl; }
        public Object getPayload() { return payload; }
    }

    public static class SagaResult {
        private final String sagaId;
        private final java.util.List<StepResult> completedSteps = new java.util.ArrayList<>();
        private boolean failed = false;
        private String error;

        public SagaResult(String sagaId) {
            this.sagaId = sagaId;
        }

        public void addStepResult(String stepId, Object result) {
            completedSteps.add(new StepResult(stepId, result));
        }

        // Getters and setters
        public String getSagaId() { return sagaId; }
        public java.util.List<StepResult> getCompletedSteps() { return completedSteps; }
        public boolean isFailed() { return failed; }
        public void setFailed(boolean failed) { this.failed = failed; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class StepResult {
        private final String stepId;
        private final Object result;

        public StepResult(String stepId, Object result) {
            this.stepId = stepId;
            this.result = result;
        }

        // Getters
        public String getStepId() { return stepId; }
        public Object getResult() { return result; }
    }
}
