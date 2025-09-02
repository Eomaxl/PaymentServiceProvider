package com.paymentprovider.paymentservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for payment API
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        logger.warn("Payment not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "PAYMENT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentValidation(PaymentValidationException ex) {
        logger.warn("Payment validation error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "PAYMENT_VALIDATION_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessing(PaymentProcessingException ex) {
        logger.error("Payment processing error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "PAYMENT_PROCESSING_ERROR",
                ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                HttpStatus.BAD_REQUEST.value(),
                Instant.now(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        private String code;
        private String message;
        private int status;
        private Instant timestamp;
        private Map<String, String> details;

        public ErrorResponse(String code, String message, int status, Instant timestamp) {
            this.code = code;
            this.message = message;
            this.status = status;
            this.timestamp = timestamp;
        }

        public ErrorResponse(String code, String message, int status, Instant timestamp, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.status = status;
            this.timestamp = timestamp;
            this.details = details;
        }

        // Getters and setters
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, String> getDetails() {
            return details;
        }

        public void setDetails(Map<String, String> details) {
            this.details = details;
        }
    }
}
