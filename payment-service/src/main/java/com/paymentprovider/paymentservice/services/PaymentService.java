package com.paymentprovider.paymentservice.services;

import com.paymentprovider.paymentservice.domain.*;
import com.paymentprovider.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core payment processing service with business logic
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, IdempotencyService idempotencyService) {
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Creates a new payment with validation and idempotency handling
     */
    public Payment createPayment(PaymentRequest request, String idempotencyKey) {
        logger.info("Creating payment for merchant: {}, amount: {}, currency: {}",
                request.getMerchantId(), request.getAmount(), request.getCurrency());

        // Check idempotency
        if (idempotencyKey != null) {
            Optional<Payment> existingPayment = idempotencyService.getExistingPayment(idempotencyKey);
            if (existingPayment.isPresent()) {
                logger.info("Returning existing payment for idempotency key: {}", idempotencyKey);
                return existingPayment.get();
            }
        }

        // Validate payment request
        validatePaymentRequest(request);

        // Create payment entity
        Payment payment = new Payment(
                request.getMerchantId(),
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethod()
        );

        // Set additional fields
        payment.setDescription(request.getDescription());
        payment.setCustomerId(request.getCustomerId());
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setReturnUrl(request.getReturnUrl());
        payment.setWebhookUrl(request.getWebhookUrl());
        payment.setMetadata(request.getMetadata());

        // Set payment instrument data if provided
        if (request.getPaymentInstrument() != null) {
            payment.setPaymentInstrumentData(
                    PaymentInstrumentData.fromPaymentInstrument(request.getPaymentInstrument())
            );
        }

        // Set billing address data if provided
        if (request.getBillingAddress() != null) {
            payment.setBillingAddressData(
                    BillingAddressData.from(request.getBillingAddress())
            );
        }

        // Set expiration time (default 30 minutes)
        payment.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));

        // Save payment
        Payment savedPayment = paymentRepository.save(payment);

        // Store idempotency mapping
        if (idempotencyKey != null) {
            idempotencyService.storePaymentMapping(idempotencyKey, savedPayment.getPaymentId());
        }

        logger.info("Created payment with ID: {}", savedPayment.getPaymentId());
        return savedPayment;
    }

    /**
     * Retrieves a payment by ID
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPayment(String paymentId) {
        return paymentRepository.findById(paymentId);
    }

    /**
     * Retrieves payments for a merchant with pagination
     */
    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsByMerchant(String merchantId, Pageable pageable) {
        return paymentRepository.findByMerchantId(merchantId, pageable);
    }

    /**
     * Authorizes a payment
     */
    public Payment authorizePayment(String paymentId, String authorizationCode, String processorReference) {
        logger.info("Authorizing payment: {}", paymentId);

        Payment payment = getPaymentOrThrow(paymentId);

        if (!payment.canBeAuthorized()) {
            throw new PaymentProcessingException(
                    "Payment cannot be authorized in current status: " + payment.getStatus()
            );
        }

        payment.authorize(authorizationCode, processorReference);
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment {} authorized successfully", paymentId);
        return savedPayment;
    }

    /**
     * Captures an authorized payment
     */
    public Payment capturePayment(String paymentId) {
        logger.info("Capturing payment: {}", paymentId);

        Payment payment = getPaymentOrThrow(paymentId);

        if (!payment.canBeCaptured()) {
            throw new PaymentProcessingException(
                    "Payment cannot be captured in current status: " + payment.getStatus()
            );
        }

        payment.capture();
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment {} captured successfully", paymentId);
        return savedPayment;
    }

    /**
     * Captures an authorized payment with a specific amount (partial capture)
     */
    public Payment capturePayment(String paymentId, BigDecimal captureAmount) {
        logger.info("Capturing payment: {} with amount: {}", paymentId, captureAmount);

        Payment payment = getPaymentOrThrow(paymentId);

        if (!payment.canBeCaptured()) {
            throw new PaymentProcessingException(
                    "Payment cannot be captured in current status: " + payment.getStatus()
            );
        }

        if (captureAmount.compareTo(payment.getAmount()) > 0) {
            throw new PaymentValidationException(
                    "Capture amount cannot exceed authorized amount: " + payment.getAmount()
            );
        }

        if (captureAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException(
                    "Capture amount must be greater than zero"
            );
        }

        // For partial capture, update the amount
        if (captureAmount.compareTo(payment.getAmount()) < 0) {
            payment.setAmount(captureAmount);
        }

        payment.capture();
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment {} captured successfully with amount: {}", paymentId, captureAmount);
        return savedPayment;
    }

    /**
     * Fails a payment with reason and code
     */
    public Payment failPayment(String paymentId, String failureReason, String failureCode) {
        logger.info("Failing payment: {} with reason: {}", paymentId, failureReason);

        Payment payment = getPaymentOrThrow(paymentId);
        payment.fail(failureReason, failureCode);
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment {} failed", paymentId);
        return savedPayment;
    }

    /**
     * Declines a payment with reason and code
     */
    public Payment declinePayment(String paymentId, String failureReason, String failureCode) {
        logger.info("Declining payment: {} with reason: {}", paymentId, failureReason);

        Payment payment = getPaymentOrThrow(paymentId);
        payment.decline(failureReason, failureCode);
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment {} declined", paymentId);
        return savedPayment;
    }

    /**
     * Updates payment status
     */
    public Payment updatePaymentStatus(String paymentId, PaymentStatus newStatus) {
        logger.info("Updating payment {} status to: {}", paymentId, newStatus);

        Payment payment = getPaymentOrThrow(paymentId);
        payment.setStatus(newStatus);
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment {} status updated to: {}", paymentId, newStatus);
        return savedPayment;
    }

    /**
     * Finds payments by status
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    /**
     * Finds expired payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getExpiredPayments() {
        return paymentRepository.findExpiredPayments(PaymentStatus.PENDING, Instant.now());
    }

    /**
     * Calculates total amount for merchant by status
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalAmountByMerchantAndStatus(String merchantId, PaymentStatus status) {
        return paymentRepository.sumAmountByMerchantIdAndStatus(merchantId, status);
    }

    /**
     * Counts payments by merchant and status
     */
    @Transactional(readOnly = true)
    public long countPaymentsByMerchantAndStatus(String merchantId, PaymentStatus status) {
        return paymentRepository.countByMerchantIdAndStatus(merchantId, status);
    }

    /**
     * Checks if payment exists by merchant and external reference
     */
    @Transactional(readOnly = true)
    public boolean paymentExistsByMerchantAndExternalReference(String merchantId, String externalReference) {
        return paymentRepository.existsByMerchantIdAndExternalReference(merchantId, externalReference);
    }

    private Payment getPaymentOrThrow(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getMerchantId() == null || request.getMerchantId().trim().isEmpty()) {
            throw new PaymentValidationException("Merchant ID is required");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Amount must be greater than zero");
        }

        if (request.getCurrency() == null) {
            throw new PaymentValidationException("Currency is required");
        }

        if (request.getPaymentMethod() == null) {
            throw new PaymentValidationException("Payment method is required");
        }

        // Validate amount precision (max 2 decimal places)
        if (request.getAmount().scale() > 2) {
            throw new PaymentValidationException("Amount cannot have more than 2 decimal places");
        }

        // Validate amount range (max 999,999.99)
        if (request.getAmount().compareTo(new BigDecimal("999999.99")) > 0) {
            throw new PaymentValidationException("Amount exceeds maximum allowed value");
        }

        // Validate email format if provided
        if (request.getCustomerEmail() != null && !isValidEmail(request.getCustomerEmail())) {
            throw new PaymentValidationException("Invalid customer email format");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
