package com.paymentprovider.paymentservice.services;

import com.paymentprovider.currency.dto.CurrencyConversionResponse;
import com.paymentprovider.paymentservice.domain.*;
import com.paymentprovider.paymentservice.repository.MerchantRepository;
import com.paymentprovider.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Enhanced payment service with multi-currency support
 */
@Service
@Transactional
public class MultiCurrencyPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(MultiCurrencyPaymentService.class);

    private final PaymentService paymentService;
    private final CurrencyServiceClient currencyServiceClient;
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public MultiCurrencyPaymentService(PaymentService paymentService,
                                       CurrencyServiceClient currencyServiceClient,
                                       MerchantRepository merchantRepository,
                                       PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.currencyServiceClient = currencyServiceClient;
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Creates a payment with currency validation and conversion support
     */
    public Payment createPaymentWithCurrencySupport(PaymentRequest request, String idempotencyKey) {
        logger.info("Creating multi-currency payment for merchant: {}, amount: {}, currency: {}",
                request.getMerchantId(), request.getAmount(), request.getCurrency());

        // Validate currency support
        validateCurrencySupport(request.getCurrency());

        // Get merchant configuration
        Merchant merchant = getMerchantOrThrow(request.getMerchantId());

        // Create the payment
        Payment payment = paymentService.createPayment(request, idempotencyKey);

        // Add currency conversion information if needed
        if (merchant.requiresCurrencyConversion(request.getCurrency())) {
            addCurrencyConversionInfo(payment, merchant);
        }

        return payment;
    }

    /**
     * Processes payment with currency conversion if required
     */
    public PaymentProcessingResult processPaymentWithCurrencyConversion(String paymentId) {
        logger.info("Processing payment with currency conversion: {}", paymentId);

        Payment payment = getPaymentOrThrow(paymentId);
        Merchant merchant = getMerchantOrThrow(payment.getMerchantId());

        PaymentProcessingResult result = new PaymentProcessingResult();
        result.setPayment(payment);
        result.setOriginalAmount(payment.getAmount());
        result.setOriginalCurrency(payment.getCurrency());

        // Check if currency conversion is needed
        if (merchant.requiresCurrencyConversion(payment.getCurrency())) {
            logger.info("Currency conversion required from {} to {} for payment: {}",
                    payment.getCurrency(), merchant.getSettlementCurrency(), paymentId);

            try {
                CurrencyConversionResponse conversion = currencyServiceClient.convertCurrency(
                        payment.getAmount(),
                        payment.getCurrency().getCode(),
                        merchant.getSettlementCurrency().getCode()
                );

                result.setConversionApplied(true);
                result.setConvertedAmount(conversion.getTotalAmount());
                result.setConvertedCurrency(Currency.fromCode(conversion.getToCurrency()));
                result.setExchangeRate(conversion.getExchangeRate());
                result.setConversionFee(conversion.getConversionFee());

                // Update payment metadata with conversion info
                payment.getMetadata().put("conversion_applied", "true");
                payment.getMetadata().put("original_amount", payment.getAmount().toString());
                payment.getMetadata().put("original_currency", payment.getCurrency().getCode());
                payment.getMetadata().put("converted_amount", conversion.getTotalAmount().toString());
                payment.getMetadata().put("converted_currency", conversion.getToCurrency());
                payment.getMetadata().put("exchange_rate", conversion.getExchangeRate().toString());
                payment.getMetadata().put("conversion_fee", conversion.getConversionFee().toString());

                paymentRepository.save(payment);

                logger.info("Currency conversion completed for payment: {} - {} {} converted to {} {}",
                        paymentId, payment.getAmount(), payment.getCurrency(),
                        conversion.getTotalAmount(), conversion.getToCurrency());

            } catch (Exception e) {
                logger.error("Currency conversion failed for payment: {}", paymentId, e);
                result.setConversionError(e.getMessage());
                throw new PaymentProcessingException("Currency conversion failed: " + e.getMessage(), e);
            }
        } else {
            result.setConversionApplied(false);
            result.setConvertedAmount(payment.getAmount());
            result.setConvertedCurrency(payment.getCurrency());
        }

        return result;
    }

    /**
     * Validates merchant settlement currency configuration
     */
    public void validateMerchantCurrencyConfiguration(String merchantId, Currency paymentCurrency) {
        Merchant merchant = getMerchantOrThrow(merchantId);

        // Validate that both currencies are supported
        validateCurrencySupport(paymentCurrency);
        validateCurrencySupport(merchant.getSettlementCurrency());

        // Check if conversion is available if needed
        if (merchant.requiresCurrencyConversion(paymentCurrency)) {
            try {
                var exchangeRate = currencyServiceClient.getExchangeRate(
                        paymentCurrency.getCode(),
                        merchant.getSettlementCurrency().getCode()
                );

                if (exchangeRate == null) {
                    throw new PaymentValidationException(
                            "Exchange rate not available from " + paymentCurrency + " to " + merchant.getSettlementCurrency()
                    );
                }
            } catch (Exception e) {
                throw new PaymentValidationException(
                        "Currency conversion validation failed: " + e.getMessage(), e
                );
            }
        }
    }

    /**
     * Gets settlement amount for a payment considering currency conversion
     */
    @Transactional(readOnly = true)
    public SettlementAmount getSettlementAmount(String paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        Merchant merchant = getMerchantOrThrow(payment.getMerchantId());

        SettlementAmount settlementAmount = new SettlementAmount();
        settlementAmount.setPaymentId(paymentId);
        settlementAmount.setOriginalAmount(payment.getAmount());
        settlementAmount.setOriginalCurrency(payment.getCurrency());

        if (merchant.requiresCurrencyConversion(payment.getCurrency())) {
            // Check if conversion info is already stored in metadata
            if (payment.getMetadata().containsKey("converted_amount")) {
                settlementAmount.setSettlementAmount(new BigDecimal(payment.getMetadata().get("converted_amount")));
                settlementAmount.setSettlementCurrency(Currency.fromCode(payment.getMetadata().get("converted_currency")));
                settlementAmount.setExchangeRate(new BigDecimal(payment.getMetadata().get("exchange_rate")));
                settlementAmount.setConversionFee(new BigDecimal(payment.getMetadata().get("conversion_fee")));
            } else {
                // Calculate conversion on-demand
                try {
                    CurrencyConversionResponse conversion = currencyServiceClient.convertCurrency(
                            payment.getAmount(),
                            payment.getCurrency().getCode(),
                            merchant.getSettlementCurrency().getCode()
                    );

                    settlementAmount.setSettlementAmount(conversion.getTotalAmount());
                    settlementAmount.setSettlementCurrency(Currency.fromCode(conversion.getToCurrency()));
                    settlementAmount.setExchangeRate(conversion.getExchangeRate());
                    settlementAmount.setConversionFee(conversion.getConversionFee());
                } catch (Exception e) {
                    logger.error("Failed to calculate settlement amount for payment: {}", paymentId, e);
                    throw new PaymentProcessingException("Failed to calculate settlement amount", e);
                }
            }
        } else {
            settlementAmount.setSettlementAmount(payment.getAmount());
            settlementAmount.setSettlementCurrency(payment.getCurrency());
            settlementAmount.setExchangeRate(BigDecimal.ONE);
            settlementAmount.setConversionFee(BigDecimal.ZERO);
        }

        return settlementAmount;
    }

    private void validateCurrencySupport(Currency currency) {
        if (!currencyServiceClient.isCurrencySupported(currency.getCode())) {
            throw new PaymentValidationException("Currency not supported: " + currency.getCode());
        }
    }

    private Merchant getMerchantOrThrow(String merchantId) {
        return merchantRepository.findActiveMerchant(merchantId)
                .orElseThrow(() -> new PaymentValidationException("Active merchant not found: " + merchantId));
    }

    private Payment getPaymentOrThrow(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private void addCurrencyConversionInfo(Payment payment, Merchant merchant) {
        payment.getMetadata().put("requires_conversion", "true");
        payment.getMetadata().put("settlement_currency", merchant.getSettlementCurrency().getCode());
        payment.getMetadata().put("auto_conversion", merchant.getAutoCurrencyConversion().toString());
    }
}
