package com.digital.lending.loanproduct.repository;

import com.digital.lending.loanproduct.model.LoanProductFamilyDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanProductFamilyDefinitionRepository extends JpaRepository<LoanProductFamilyDefinition, String> {
    boolean existsByFamilyCodeAndIsActiveTrue(String familyCode);
    Optional<LoanProductFamilyDefinition> findByFamilyCode(String familyCode);
}