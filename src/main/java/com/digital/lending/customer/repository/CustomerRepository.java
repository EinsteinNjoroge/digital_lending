package com.digital.lending.customer.repository;

import com.digital.lending.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(i) > 0 FROM IndividualCustomer c JOIN c.identities i WHERE i.documentNumber = :docNum")
    boolean existsInIndividualIdentities(@Param("docNum") String docNum);

    @Query("SELECT COUNT(d) > 0 FROM CorporateCustomer c JOIN c.directorIdentities d WHERE d.documentNumber = :docNum")
    boolean existsInCorporateIdentities(@Param("docNum") String docNum);

    @Query("SELECT COUNT(a) > 0 FROM JointCustomer c JOIN c.applicantIdentities a WHERE a.documentNumber = :docNum")
    boolean existsInJointIdentities(@Param("docNum") String docNum);
}