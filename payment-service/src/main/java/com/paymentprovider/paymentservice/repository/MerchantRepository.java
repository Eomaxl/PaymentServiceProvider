package com.paymentprovider.paymentservice.repository;

import com.paymentprovider.paymentservice.domain.Merchant;
import com.paymentprovider.paymentservice.domain.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {

    @Query("SELECT m FROM Merchant m WHERE m.status = :status")
    List<Merchant> findByStatus(@Param("status") MerchantStatus status);

    @Query("SELECT m FROM Merchant m WHERE m.merchantId = :merchantId AND m.status = 'ACTIVE'")
    Optional<Merchant> findActiveMerchant(@Param("merchantId") String merchantId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Merchant m WHERE m.merchantId = :merchantId AND m.status = 'ACTIVE'")
    boolean existsActiveMerchant(@Param("merchantId") String merchantId);
}
