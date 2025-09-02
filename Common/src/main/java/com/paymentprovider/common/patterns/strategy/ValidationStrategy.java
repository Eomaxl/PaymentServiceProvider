package com.paymentprovider.common.patterns.strategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strategy pattern for different validation strategies.
 * Follows Strategy Pattern, Single Responsibility Principle, and Open/Closed Principle.
 */
public interface ValidationStrategy<T> {
    ValidationResult validate(T input);
    String getValidationType();
}

/**
 * Validation result
 */
class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : List.of();
    }

    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(String... errors) {
        return new ValidationResult(false, List.of(errors));
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
}

/**
 * Email validation strategy
 */
class EmailValidationStrategy implements ValidationStrategy<String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    @Override
    public ValidationResult validate(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.failure("Email is required");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.failure("Invalid email format");
        }

        return ValidationResult.success();
    }

    @Override
    public String getValidationType() {
        return "email";
    }
}

/**
 * Amount validation strategy
 */
class AmountValidationStrategy implements ValidationStrategy<BigDecimal> {

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    public AmountValidationStrategy(BigDecimal minAmount, BigDecimal maxAmount) {
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    @Override
    public ValidationResult validate(BigDecimal amount) {
        if (amount == null) {
            return ValidationResult.failure("Amount is required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.failure("Amount must be positive");
        }

        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return ValidationResult.failure("Amount is below minimum: " + minAmount);
        }

        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            return ValidationResult.failure("Amount exceeds maximum: " + maxAmount);
        }

        return ValidationResult.success();
    }

    @Override
    public String getValidationType() {
        return "amount";
    }
}

/**
 * Credit card validation strategy
 */
class CreditCardValidationStrategy implements ValidationStrategy<String> {

    @Override
    public ValidationResult validate(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return ValidationResult.failure("Card number is required");
        }

        String cleanCardNumber = cardNumber.replaceAll("\\s|-", "");

        if (!cleanCardNumber.matches("\\d+")) {
            return ValidationResult.failure("Card number must contain only digits");
        }

        if (cleanCardNumber.length() < 13 || cleanCardNumber.length() > 19) {
            return ValidationResult.failure("Card number must be between 13 and 19 digits");
        }

        if (!isValidLuhn(cleanCardNumber)) {
            return ValidationResult.failure("Invalid card number");
        }

        return ValidationResult.success();
    }

    @Override
    public String getValidationType() {
        return "credit-card";
    }

    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}

/**
 * Currency validation strategy
 */
class CurrencyValidationStrategy implements ValidationStrategy<String> {

    private static final List<String> SUPPORTED_CURRENCIES = List.of(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "SEK", "NZD"
    );

    @Override
    public ValidationResult validate(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            return ValidationResult.failure("Currency is required");
        }

        String upperCurrency = currency.toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(upperCurrency)) {
            return ValidationResult.failure("Unsupported currency: " + currency);
        }

        return ValidationResult.success();
    }

    @Override
    public String getValidationType() {
        return "currency";
    }
}

/**
 * Composite validation strategy that combines multiple strategies
 */
class CompositeValidationStrategy<T> implements ValidationStrategy<T> {

    private final List<ValidationStrategy<T>> strategies;
    private final String validationType;

    public CompositeValidationStrategy(String validationType, List<ValidationStrategy<T>> strategies) {
        this.validationType = validationType;
        this.strategies = strategies;
    }

    @Override
    public ValidationResult validate(T input) {
        for (ValidationStrategy<T> strategy : strategies) {
            ValidationResult result = strategy.validate(input);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.success();
    }

    @Override
    public String getValidationType() {
        return validationType;
    }
}