package com.digital.lending.creditscoring.repository;

import com.digital.lending.creditscoring.model.CreditProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditProfileRepository extends JpaRepository<CreditProfile, String> {
}
