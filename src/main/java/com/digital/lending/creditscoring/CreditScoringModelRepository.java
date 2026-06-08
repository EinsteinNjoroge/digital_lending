package com.digital.lending.creditscoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditScoringModelRepository extends JpaRepository<CreditScoringModelDefinition, String> {
    @Query("SELECT m FROM CreditScoringModelDefinition m " +
            "WHERE m.partnerId = :partnerId " +
            "AND m.currency = :currency " +
            "AND m.modelCode = :modelCode " +
            "AND m.isActive = true")
    Optional<CreditScoringModelDefinition> findActiveModel(
            @Param("partnerId") String partnerId,
            @Param("currency") String currency,
            @Param("modelCode") String modelCode
    );

    List<CreditScoringModelDefinition> findByPartnerIdAndIsActive(String partnerId, boolean isActive);

    List<CreditScoringModelDefinition> findByPartnerId(String partnerId);

    List<CreditScoringModelDefinition> findByIsActive(boolean isActive);
}