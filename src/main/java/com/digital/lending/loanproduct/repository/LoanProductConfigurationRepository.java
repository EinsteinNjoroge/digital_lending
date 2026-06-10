package com.digital.lending.loanproduct.repository;

import com.digital.lending.loanproduct.model.LoanProductConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanProductConfigurationRepository extends JpaRepository<LoanProductConfiguration, String> {

    @Query("SELECT COALESCE(MAX(c.version), 0) FROM LoanProductConfiguration c WHERE c.productCode = :productCode")
    int findMaxVersionByProductCode(@Param("productCode") String productCode);

    boolean existsByPartnerIdAndCurrencyAndProductCodeAndIsActiveTrue(String partnerId, String currency, String productCode);

    @Query("SELECT c FROM LoanProductConfiguration c " +
            "WHERE (:partnerId IS NULL OR c.partnerId = :partnerId) " +
            "AND (:currency IS NULL OR c.currency = :currency) " +
            "AND (:isActive IS NULL OR c.isActive = :isActive)")
    List<LoanProductConfiguration> findByFilters(
            @Param("partnerId") String partnerId,
            @Param("currency") String currency,
            @Param("isActive") Boolean isActive
    );
}
