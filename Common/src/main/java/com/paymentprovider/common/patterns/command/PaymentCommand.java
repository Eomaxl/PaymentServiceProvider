package com.paymentprovider.common.patterns.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Command pattern implementation for payment operations.
 * Follows Command Pattern, Single Responsibility Principle, and supports undo operations.
 */
public interface Command<T> {
    CompletableFuture<T> execute();
    CompletableFuture<Void> undo();
    String getCommandId();
    String getCommandType();
    boolean canUndo();
}

/**
 * Abstract base command with common functionality
 */
public abstract class BaseCommand<T> implements Command<T> {

    protected final String commandId;
    protected final String commandType;
    protected final Instant createdAt;
    protected volatile boolean executed = false;
    protected volatile T result;

    protected BaseCommand(String commandType) {
        this.commandId = java.util.UUID.randomUUID().toString();
        this.commandType = commandType;
        this.createdAt = Instant.now();
    }

    @Override
    public String getCommandId() {
        return commandId;
    }

    @Override
    public String getCommandType() {
        return commandType;
    }

    @Override
    public boolean canUndo() {
        return executed && result != null;
    }

    protected abstract CompletableFuture<T> doExecute();
    protected abstract CompletableFuture<Void> doUndo();

    @Override
    public CompletableFuture<T> execute() {
        if (executed) {
            return CompletableFuture.completedFuture(result);
        }

        return doExecute().thenApply(res -> {
            this.result = res;
            this.executed = true;
            return res;
        });
    }

    @Override
    public CompletableFuture<Void> undo() {
        if (!canUndo()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Command cannot be undone: " + commandId));
        }
        return doUndo();
    }
}

/**
 * Command to process a payment
 */
public class ProcessPaymentCommand extends BaseCommand<PaymentResult> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPaymentCommand.class);

    private final PaymentRequest paymentRequest;
    private final PaymentProcessor processor;

    public ProcessPaymentCommand(PaymentRequest paymentRequest, PaymentProcessor processor) {
        super("PROCESS_PAYMENT");
        this.paymentRequest = paymentRequest;
        this.processor = processor;
    }

    @Override
    protected CompletableFuture<PaymentResult> doExecute() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing payment command: {} for payment: {}", commandId, paymentRequest.getPaymentId());

            try {
                // Process the payment
                PaymentResult result = processor.processPayment(paymentRequest);
                logger.info("Payment command executed successfully: {}", commandId);
                return result;
            } catch (Exception e) {
                logger.error("Payment command execution failed: {}", commandId, e);
                throw new RuntimeException("Payment processing failed", e);
            }
        });
    }

    @Override
    protected CompletableFuture<Void> doUndo() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Undoing payment command: {} for payment: {}", commandId, paymentRequest.getPaymentId());

            try {
                // Reverse the payment (refund)
                processor.refundPayment(result.getTransactionId());
                logger.info("Payment command undone successfully: {}", commandId);
            } catch (Exception e) {
                logger.error("Payment command undo failed: {}", commandId, e);
                throw new RuntimeException("Payment undo failed", e);
            }
        });
    }
}

/**
 * Command to validate payment data
 */
public class ValidatePaymentCommand extends BaseCommand<ValidationResult> {

    private static final Logger logger = LoggerFactory.getLogger(ValidatePaymentCommand.class);

    private final PaymentRequest paymentRequest;
    private final PaymentValidator validator;

    public ValidatePaymentCommand(PaymentRequest paymentRequest, PaymentValidator validator) {
        super("VALIDATE_PAYMENT");
        this.paymentRequest = paymentRequest;
        this.validator = validator;
    }

    @Override
    protected CompletableFuture<ValidationResult> doExecute() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing validation command: {} for payment: {}", commandId, paymentRequest.getPaymentId());

            ValidationResult result = validator.validate(paymentRequest);
            logger.info("Validation command executed: {} - Valid: {}", commandId, result.isValid());
            return result;
        });
    }

    @Override
    protected CompletableFuture<Void> doUndo() {
        // Validation doesn't need undo
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean canUndo() {
        return false; // Validation commands cannot be undone
    }
}

/**
 * Command invoker that manages command execution
 */
@Component
public class CommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(CommandInvoker.class);

    private final ConcurrentHashMap<String, Command<?>> executedCommands = new ConcurrentHashMap<>();
    private final Executor commandExecutor;

    public CommandInvoker(Executor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * Execute a command
     */
    public <T> CompletableFuture<T> execute(Command<T> command) {
        logger.debug("Invoking command: {} of type: {}", command.getCommandId(), command.getCommandType());

        return command.execute().whenComplete((result, throwable) -> {
            if (throwable == null) {
                executedCommands.put(command.getCommandId(), command);
                logger.debug("Command executed successfully: {}", command.getCommandId());
            } else {
                logger.error("Command execution failed: {}", command.getCommandId(), throwable);
            }
        });
    }

    /**
     * Undo a command by ID
     */
    public CompletableFuture<Void> undo(String commandId) {
        Command<?> command = executedCommands.get(commandId);
        if (command == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Command not found: " + commandId));
        }

        logger.debug("Undoing command: {}", commandId);
        return command.undo().whenComplete((result, throwable) -> {
            if (throwable == null) {
                executedCommands.remove(commandId);
                logger.debug("Command undone successfully: {}", commandId);
            } else {
                logger.error("Command undo failed: {}", commandId, throwable);
            }
        });
    }

    /**
     * Get command history
     */
    public java.util.Set<String> getExecutedCommandIds() {
        return executedCommands.keySet();
    }

    /**
     * Clear command history
     */
    public void clearHistory() {
        executedCommands.clear();
        logger.debug("Command history cleared");
    }
}

// Supporting classes
class PaymentResult {
    private final String transactionId;
    private final String status;

    public PaymentResult(String transactionId, String status) {
        this.transactionId = transactionId;
        this.status = status;
    }

    public String getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
}

class ValidationResult {
    private final boolean valid;
    private final java.util.List<String> errors;

    public ValidationResult(boolean valid, java.util.List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public java.util.List<String> getErrors() { return errors; }
}

interface PaymentProcessor {
    PaymentResult processPayment(PaymentRequest request);
    void refundPayment(String transactionId);
}

interface PaymentValidator {
    ValidationResult validate(PaymentRequest request);
}

class PaymentRequest {
    private final String paymentId;
    private final java.math.BigDecimal amount;
    private final String currency;

    public PaymentRequest(String paymentId, java.math.BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getPaymentId() { return paymentId; }
    public java.math.BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}