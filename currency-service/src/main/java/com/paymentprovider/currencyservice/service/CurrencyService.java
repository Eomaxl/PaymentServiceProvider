package com.paymentprovider.currencyservice.service;

import com.paymentprovider.currencyservice.domain.Currency;
import com.paymentprovider.currencyservice.domain.ExchangeRate;
import com.paymentprovider.currencyservice.dto.CurrencyConversionRequest;
import com.paymentprovider.currencyservice.dto.CurrencyConversionResponse;
import com.paymentprovider.currencyservice.dto.ExchangeRateResponse;
import com.paymentprovider.currencyservice.provider.ExchangeRateProvider;
import com.paymentprovider.currencyservice.repository.CurrencyRepository;
import com.paymentprovider.currencyservice.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    @Value("${currency.conversion.fee.percentage:0.025}")
    private BigDecimal conversionFeePercentage;

    @Value("${currency.conversion.fee.minimum:0.10}")
    private BigDecimal minimumConversionFee;

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateProvider exchangeRateProvider;

    public CurrencyService(CurrencyRepository currencyRepository,
                           ExchangeRateRepository exchangeRateRepository,
                           ExchangeRateProvider exchangeRateProvider) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.exchangeRateProvider = exchangeRateProvider;
    }

    /**
     * Convert currency amount with fee calculation
     */
    public CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request) {
        logger.info("Converting {} {} to {}", request.getAmount(), request.getFromCurrency(), request.getToCurrency());

        // Validate currencies
        validateCurrency(request.getFromCurrency());
        validateCurrency(request.getToCurrency());

        // Same currency conversion
        if (request.getFromCurrency().equals(request.getToCurrency())) {
            return createSameCurrencyResponse(request);
        }

        // Get exchange rate
        ExchangeRate exchangeRate = getExchangeRate(request.getFromCurrency(), request.getToCurrency(), request.getAsOfDate());
        if (exchangeRate == null) {
            throw new IllegalArgumentException("Exchange rate not available for " + request.getFromCurrency() + " to " + request.getToCurrency());
        }

        // Calculate conversion
        BigDecimal convertedAmount = request.getAmount().multiply(exchangeRate.getRate())
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate fees
        BigDecimal conversionFee = BigDecimal.ZERO;
        BigDecimal totalAmount = convertedAmount;

        if (request.isIncludeFees()) {
            conversionFee = calculateConversionFee(convertedAmount);
            totalAmount = convertedAmount.add(conversionFee);
        }

        CurrencyConversionResponse response = new CurrencyConversionResponse();
        response.setOriginalAmount(request.getAmount());
        response.setFromCurrency(request.getFromCurrency());
        response.setToCurrency(request.getToCurrency());
        response.setExchangeRate(exchangeRate.getRate());
        response.setConvertedAmount(convertedAmount);
        response.setConversionFee(conversionFee);
        response.setTotalAmount(totalAmount);
        response.setRateDate(exchangeRate.getRateDate());
        response.setRateProvider(exchangeRate.getSourceProvider());

        logger.info("Currency conversion completed: {} {} = {} {} (fee: {})",
                request.getAmount(), request.getFromCurrency(),
                totalAmount, request.getToCurrency(), conversionFee);

        return response;
    }

    /**
     * Get current exchange rate between two currencies
     */
    @Transactional(readOnly = true)
    public ExchangeRateResponse getCurrentExchangeRate(String fromCurrency, String toCurrency) {
        ExchangeRate rate = getExchangeRate(fromCurrency, toCurrency, null);
        if (rate == null) {
            return null;
        }

        return new ExchangeRateResponse(rate.getFromCurrency(), rate.getToCurrency(),
                rate.getRate(), rate.getRateDate());
    }

    /**
     * Get all supported currencies
     */
    @Transactional(readOnly = true)
    public List<Currency> getSupportedCurrencies() {
        return currencyRepository.findAllActive();
    }

    /**
     * Update exchange rates from external provider
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updateExchangeRates() {
        logger.info("Starting scheduled exchange rate update");

        if (!exchangeRateProvider.isAvailable()) {
            logger.warn("Exchange rate provider is not available, skipping update");
            return;
        }

        List<Currency> currencies = currencyRepository.findAllActive();
        Instant now = Instant.now();

        for (Currency baseCurrency : currencies) {
            try {
                Map<String, BigDecimal> rates = exchangeRateProvider.getCurrentRates(baseCurrency.getCode());

                for (Map.Entry<String, BigDecimal> entry : rates.entrySet()) {
                    String targetCurrency = entry.getKey();
                    BigDecimal rate = entry.getValue();

                    if (!baseCurrency.getCode().equals(targetCurrency) &&
                            currencyRepository.existsActiveByCode(targetCurrency)) {

                        saveExchangeRate(baseCurrency.getCode(), targetCurrency, rate, now);
                    }
                }

            } catch (Exception e) {
                logger.error("Failed to update exchange rates for base currency: {}", baseCurrency.getCode(), e);
            }
        }

        logger.info("Exchange rate update completed");
    }

    private void validateCurrency(String currencyCode) {
        if (!currencyRepository.existsActiveByCode(currencyCode)) {
            throw new IllegalArgumentException("Unsupported currency: " + currencyCode);
        }
    }

    private CurrencyConversionResponse createSameCurrencyResponse(CurrencyConversionRequest request) {
        CurrencyConversionResponse response = new CurrencyConversionResponse();
        response.setOriginalAmount(request.getAmount());
        response.setFromCurrency(request.getFromCurrency());
        response.setToCurrency(request.getToCurrency());
        response.setExchangeRate(BigDecimal.ONE);
        response.setConvertedAmount(request.getAmount());
        response.setConversionFee(BigDecimal.ZERO);
        response.setTotalAmount(request.getAmount());
        response.setRateDate(Instant.now());
        response.setRateProvider("Internal");
        return response;
    }

    private ExchangeRate getExchangeRate(String fromCurrency, String toCurrency, Instant asOfDate) {
        Optional<ExchangeRate> rateOpt;

        if (asOfDate != null) {
            List<ExchangeRate> rates = exchangeRateRepository.findRatesAsOf(fromCurrency, toCurrency, asOfDate);
            rateOpt = rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(0));
        } else {
            rateOpt = exchangeRateRepository.findLatestRate(fromCurrency, toCurrency);
        }

        return rateOpt.orElse(null);
    }

    private BigDecimal calculateConversionFee(BigDecimal amount) {
        BigDecimal percentageFee = amount.multiply(conversionFeePercentage);
        return percentageFee.max(minimumConversionFee).setScale(2, RoundingMode.HALF_UP);
    }

    private void saveExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, Instant rateDate) {
        try {
            ExchangeRate exchangeRate = new ExchangeRate(fromCurrency, toCurrency, rate, rateDate);
            exchangeRate.setSourceProvider(exchangeRateProvider.getProviderName());
            exchangeRateRepository.save(exchangeRate);

            logger.debug("Saved exchange rate: {} {} = {}", fromCurrency, toCurrency, rate);
        } catch (Exception e) {
            logger.error("Failed to save exchange rate: {} to {}", fromCurrency, toCurrency, e);
        }
    }
}