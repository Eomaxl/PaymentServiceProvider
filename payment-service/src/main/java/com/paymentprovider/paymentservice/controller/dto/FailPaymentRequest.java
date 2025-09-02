package com.paymentprovider.paymentservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for failing or declining a payment
 */
public class FailPaymentRequest {

    @NotBlank(message = "Failure reason is required")
    @Size(max = 255, message = "Failure reason must not exceed 255 characters")
    private String failureReason;

    @Size(max = 50, message = "Failure code must not exceed 50 characters")
    private String failureCode;

    // Constructors
    public FailPaymentRequest() {}

    public FailPaymentRequest(String failureReason, String failureCode) {
        this.failureReason = failureReason;
        this.failureCode = failureCode;
    }

    // Getters and setters
    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }
}
