package com.digital.lending.customer.model;

import com.digital.lending.customer.enums.CustomerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_corporate_customer")
@DiscriminatorValue("CORPORATE")
@Getter
@Setter
public class CorporateCustomer extends Customer {

    public CorporateCustomer() {
        super.setCustomerType(CustomerType.CORPORATE);
    }

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Column(name = "incorporation_date", nullable = false)
    private LocalDate incorporationDate;

    @Column(name = "authorized_signatory_name", nullable = false)
    private String authorizedSignatoryName;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_corporate_director_identities", joinColumns = @JoinColumn(name = "customer_id"))
    private List<IdentityDocument> directorIdentities = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return companyName + " (LLC)";
    }
}