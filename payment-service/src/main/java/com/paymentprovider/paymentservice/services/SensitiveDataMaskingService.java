package com.paymentprovider.paymentservice.services;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for masking sensitive data in logs
 */
@Service
public class SensitiveDataMaskingService {

    // Patterns for sensitive data
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("\\b\\d{3,4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{4}\\b");

    // Sensitive field names
    private static final String[] SENSITIVE_FIELDS = {
            "cardNumber", "card_number", "pan", "cvv", "cvc", "securityCode", "security_code",
            "password", "pin", "ssn", "social_security_number", "accountNumber", "account_number",
            "routingNumber", "routing_number", "iban", "swift", "token", "apiKey", "api_key"
    };

    /**
     * Mask sensitive data in log message
     */
    public String maskSensitiveData(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String maskedMessage = message;

        // Mask card numbers
        maskedMessage = CARD_NUMBER_PATTERN.matcher(maskedMessage)
                .replaceAll(match -> maskCardNumber(match.group()));

        // Mask CVV codes
        maskedMessage = CVV_PATTERN.matcher(maskedMessage)
                .replaceAll("***");

        // Mask email addresses (partially)
        maskedMessage = EMAIL_PATTERN.matcher(maskedMessage)
                .replaceAll(match -> maskEmail(match.group()));

        // Mask phone numbers
        maskedMessage = PHONE_PATTERN.matcher(maskedMessage)
                .replaceAll("***-***-****");

        // Mask SSN
        maskedMessage = SSN_PATTERN.matcher(maskedMessage)
                .replaceAll("***-**-****");

        // Mask sensitive field values in JSON-like structures
        maskedMessage = maskSensitiveFields(maskedMessage);

        return maskedMessage;
    }

    /**
     * Mask card number showing only last 4 digits
     */
    private String maskCardNumber(String cardNumber) {
        String cleanNumber = cardNumber.replaceAll("[\\s-]", "");
        if (cleanNumber.length() >= 4) {
            return "**** **** **** " + cleanNumber.substring(cleanNumber.length() - 4);
        }
        return "****";
    }

    /**
     * Mask email address showing only first character and domain
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.charAt(0) + "***@" + email.substring(atIndex + 1);
        }
        return "***@***.***";
    }

    /**
     * Mask sensitive field values in JSON-like structures
     */
    private String maskSensitiveFields(String message) {
        String maskedMessage = message;

        for (String field : SENSITIVE_FIELDS) {
            // Pattern for JSON field: "field": "value"
            Pattern jsonPattern = Pattern.compile(
                    "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"",
                    Pattern.CASE_INSENSITIVE
            );
            maskedMessage = jsonPattern.matcher(maskedMessage)
                    .replaceAll("\"" + field + "\": \"***\"");

            // Pattern for form field: field=value
            Pattern formPattern = Pattern.compile(
                    field + "=([^&\\s]+)",
                    Pattern.CASE_INSENSITIVE
            );
            maskedMessage = formPattern.matcher(maskedMessage)
                    .replaceAll(field + "=***");

            // Pattern for XML field: <field>value</field>
            Pattern xmlPattern = Pattern.compile(
                    "<" + field + ">([^<]+)</" + field + ">",
                    Pattern.CASE_INSENSITIVE
            );
            maskedMessage = xmlPattern.matcher(maskedMessage)
                    .replaceAll("<" + field + ">***</" + field + ">");
        }

        return maskedMessage;
    }

    /**
     * Check if a field name is sensitive
     */
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String lowerFieldName = fieldName.toLowerCase();
        for (String sensitiveField : SENSITIVE_FIELDS) {
            if (lowerFieldName.contains(sensitiveField.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Mask sensitive data in object for logging
     */
    public String maskObjectForLogging(Object obj) {
        if (obj == null) {
            return "null";
        }

        String objString = obj.toString();
        return maskSensitiveData(objString);
    }
}
