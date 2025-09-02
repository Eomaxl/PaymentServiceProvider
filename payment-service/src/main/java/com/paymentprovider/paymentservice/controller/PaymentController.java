package com.paymentprovider.paymentservice.controller;

import com.paymentprovider.paymentservice.controller.dto.AuthorizeRequest;
import com.paymentprovider.paymentservice.controller.dto.CaptureRequest;
import com.paymentprovider.paymentservice.controller.dto.FailPaymentRequest;
import com.paymentprovider.paymentservice.controller.dto.PageResponse;
import com.paymentprovider.paymentservice.domain.*;
import com.paymentprovider.paymentservice.service.PaymentService;
import com.paymentprovider.paymentservice.service.MultiCurrencyPaymentService;
import com.paymentprovider.paymentservice.service.PaymentProcessingResult;
import com.paymentprovider.paymentservice.service.SettlementAmount;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST API controller for payment processing
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final MultiCurrencyPaymentService multiCurrencyPaymentService;

    @Autowired
    public PaymentController(PaymentService paymentService, MultiCurrencyPaymentService multiCurrencyPaymentService) {
        this.paymentService = paymentService;
        this.multiCurrencyPaymentService = multiCurrencyPaymentService;
    }

    /**
     * Creates a new payment
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        logger.info("Creating payment for merchant: {}, amount: {}",
                request.getMerchantId(), request.getAmount());

        try {
            Payment payment = multiCurrencyPaymentService.createPaymentWithCurrencySupport(request, idempotencyKey);
            PaymentResponse response = PaymentResponse.fromPayment(payment);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating payment", e);
            throw e;
        }
    }

    /**
     * Retrieves a payment by ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        logger.info("Retrieving payment: {}", paymentId);

        Optional<Payment> payment = paymentService.getPayment(paymentId);

        if (payment.isPresent()) {
            PaymentResponse response = PaymentResponse.fromPayment(payment.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves payments for a merchant with pagination
     */
    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> getPayments(
            @RequestParam String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.info("Retrieving payments for merchant: {}, page: {}, size: {}", merchantId, page, size);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Payment> payments = paymentService.getPaymentsByMerchant(merchantId, pageable);
        Page<PaymentResponse> paymentResponses = payments.map(PaymentResponse::fromPayment);
        PageResponse<PaymentResponse> response = PageResponse.of(paymentResponses);

        return ResponseEntity.ok(response);
    }

    /**
     * Authorizes a payment
     */
    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<PaymentResponse> authorizePayment(
            @PathVariable String paymentId,
            @Valid @RequestBody AuthorizeRequest request) {

        logger.info("Authorizing payment: {}", paymentId);

        try {
            Payment payment = paymentService.authorizePayment(
                    paymentId,
                    request.getAuthorizationCode(),
                    request.getProcessorReference()
            );

            PaymentResponse response = PaymentResponse.fromPayment(payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error authorizing payment: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * Captures an authorized payment
     */
    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @PathVariable String paymentId,
            @Valid @RequestBody(required = false) CaptureRequest request) {

        logger.info("Capturing payment: {}", paymentId);

        try {
            Payment payment;
            if (request != null && request.getAmount() != null) {
                payment = paymentService.capturePayment(paymentId, request.getAmount());
            } else {
                payment = paymentService.capturePayment(paymentId);
            }

            PaymentResponse response = PaymentResponse.fromPayment(payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error capturing payment: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * Fails a payment
     */
    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody FailPaymentRequest request) {

        logger.info("Failing payment: {}", paymentId);

        try {
            Payment payment = paymentService.failPayment(
                    paymentId,
                    request.getFailureReason(),
                    request.getFailureCode()
            );

            PaymentResponse response = PaymentResponse.fromPayment(payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error failing payment: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * Declines a payment
     */
    @PostMapping("/{paymentId}/decline")
    public ResponseEntity<PaymentResponse> declinePayment(
            @PathVariable String paymentId,
            @Valid @RequestBody FailPaymentRequest request) {

        logger.info("Declining payment: {}", paymentId);

        try {
            Payment payment = paymentService.declinePayment(
                    paymentId,
                    request.getFailureReason(),
                    request.getFailureCode()
            );

            PaymentResponse response = PaymentResponse.fromPayment(payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error declining payment: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * Processes payment with currency conversion
     */
    @PostMapping("/{paymentId}/process-with-conversion")
    public ResponseEntity<PaymentProcessingResult> processPaymentWithConversion(@PathVariable String paymentId) {
        logger.info("Processing payment with currency conversion: {}", paymentId);

        try {
            PaymentProcessingResult result = multiCurrencyPaymentService.processPaymentWithCurrencyConversion(paymentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error processing payment with conversion: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * Gets settlement amount for a payment
     */
    @GetMapping("/{paymentId}/settlement-amount")
    public ResponseEntity<SettlementAmount> getSettlementAmount(@PathVariable String paymentId) {
        logger.info("Getting settlement amount for payment: {}", paymentId);

        try {
            SettlementAmount settlementAmount = multiCurrencyPaymentService.getSettlementAmount(paymentId);
            return ResponseEntity.ok(settlementAmount);
        } catch (Exception e) {
            logger.error("Error getting settlement amount for payment: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * Validates merchant currency configuration
     */
    @PostMapping("/validate-currency")
    public ResponseEntity<Void> validateCurrencyConfiguration(
            @RequestParam String merchantId,
            @RequestParam String currency) {

        logger.info("Validating currency configuration for merchant: {}, currency: {}", merchantId, currency);

        try {
            Currency paymentCurrency = Currency.fromCode(currency);
            multiCurrencyPaymentService.validateMerchantCurrencyConfiguration(merchantId, paymentCurrency);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Currency configuration validation failed for merchant: {}, currency: {}", merchantId, currency, e);
            throw e;
        }
    }
}