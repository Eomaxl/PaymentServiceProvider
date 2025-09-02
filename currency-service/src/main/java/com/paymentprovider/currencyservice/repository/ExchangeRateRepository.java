package com.paymentprovider.currencyservice.repository;

import com.paymentprovider.currencyservice.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency AND e.toCurrency = :toCurrency " +
            "AND e.isActive = true ORDER BY e.rateDate DESC")
    List<ExchangeRate> findLatestRates(@Param("fromCurrency") String fromCurrency,
                                       @Param("toCurrency") String toCurrency);

    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency AND e.toCurrency = :toCurrency " +
            "AND e.isActive = true AND e.rateDate <= :asOfDate ORDER BY e.rateDate DESC")
    List<ExchangeRate> findRatesAsOf(@Param("fromCurrency") String fromCurrency,
                                     @Param("toCurrency") String toCurrency,
                                     @Param("asOfDate") Instant asOfDate);

    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency AND e.toCurrency = :toCurrency " +
            "AND e.isActive = true ORDER BY e.rateDate DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(@Param("fromCurrency") String fromCurrency,
                                          @Param("toCurrency") String toCurrency);

    @Query("SELECT e FROM ExchangeRate e WHERE e.rateDate >= :startDate AND e.rateDate <= :endDate " +
            "AND e.isActive = true ORDER BY e.rateDate DESC")
    List<ExchangeRate> findRatesBetween(@Param("startDate") Instant startDate,
                                        @Param("endDate") Instant endDate);

    @Query("SELECT DISTINCT e.fromCurrency FROM ExchangeRate e WHERE e.isActive = true")
    List<String> findAllFromCurrencies();

    @Query("SELECT DISTINCT e.toCurrency FROM ExchangeRate e WHERE e.isActive = true")
    List<String> findAllToCurrencies();
}
