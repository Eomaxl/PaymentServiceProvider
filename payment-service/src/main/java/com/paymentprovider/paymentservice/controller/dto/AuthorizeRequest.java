package com.paymentprovider.paymentservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for payment authorization
 */
public class AuthorizeRequest {

    @NotBlank(message = "Authorization code is required")
    @Size(max = 50, message = "Authorization code must not exceed 50 characters")
    private String authorizationCode;

    @NotBlank(message = "Processor reference is required")
    @Size(max = 100, message = "Processor reference must not exceed 100 characters")
    private String processorReference;

    // Constructors
    public AuthorizeRequest() {}

    public AuthorizeRequest(String authorizationCode, String processorReference) {
        this.authorizationCode = authorizationCode;
        this.processorReference = processorReference;
    }

    // Getters and setters
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getProcessorReference() {
        return processorReference;
    }

    public void setProcessorReference(String processorReference) {
        this.processorReference = processorReference;
    }
}
