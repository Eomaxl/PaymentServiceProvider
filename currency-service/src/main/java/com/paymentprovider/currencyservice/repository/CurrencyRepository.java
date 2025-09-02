package com.paymentprovider.currencyservice.repository;

import com.paymentprovider.currencyservice.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

    @Query("SELECT c FROM Currency c WHERE c.isActive = true")
    List<Currency> findAllActive();

    @Query("SELECT c FROM Currency c WHERE c.code = :code AND c.isActive = true")
    Optional<Currency> findActiveByCode(String code);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Currency c WHERE c.code = :code AND c.isActive = true")
    boolean existsActiveByCode(String code);
}
