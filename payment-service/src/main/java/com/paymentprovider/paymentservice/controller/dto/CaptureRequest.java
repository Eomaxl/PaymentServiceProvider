package com.paymentprovider.paymentservice.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for payment capture
 */
public class CaptureRequest {

    @DecimalMin(value = "0.01", message = "Capture amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Capture amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amount;

    @Size(max = 255, message = "Capture reason must not exceed 255 characters")
    private String reason;

    @Size(max = 100, message = "Processor reference must not exceed 100 characters")
    private String processorReference;

    // Constructors
    public CaptureRequest() {}

    public CaptureRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public CaptureRequest(BigDecimal amount, String reason) {
        this.amount = amount;
        this.reason = reason;
    }

    public CaptureRequest(BigDecimal amount, String reason, String processorReference) {
        this.amount = amount;
        this.reason = reason;
        this.processorReference = processorReference;
    }

    // Getters and setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getProcessorReference() {
        return processorReference;
    }

    public void setProcessorReference(String processorReference) {
        this.processorReference = processorReference;
    }
}
