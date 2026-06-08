package com.digital.lending.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_individual_customer")
@DiscriminatorValue("INDIVIDUAL")
@Getter
@Setter
class IndividualCustomer extends Customer {

    public IndividualCustomer() {
        super.setCustomerType(CustomerType.INDIVIDUAL);
    }

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_individual_identities", joinColumns = @JoinColumn(name = "customer_id"))
    private List<IdentityDocument> identities = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }
}