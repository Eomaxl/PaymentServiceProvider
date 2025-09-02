package com.paymentprovider.paymentservice.event.replay;

import java.util.List;

/**
 * Result of event sequence validation.
 * Contains validation status and any violations found.
 */
public class EventSequenceValidationResult {

    private final boolean isValid;
    private final List<String> violations;

    public EventSequenceValidationResult(boolean isValid, List<String> violations) {
        this.isValid = isValid;
        this.violations = violations;
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> getViolations() {
        return violations;
    }

    public boolean hasViolations() {
        return violations != null && !violations.isEmpty();
    }

    public int getViolationCount() {
        return violations != null ? violations.size() : 0;
    }

    @Override
    public String toString() {
        return "EventSequenceValidationResult{" +
                "isValid=" + isValid +
                ", violationCount=" + getViolationCount() +
                ", violations=" + violations +
                '}';
    }
}
