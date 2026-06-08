package com.digital.lending.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_joint_customer")
@DiscriminatorValue("JOINT")
@Getter
@Setter
class JointCustomer extends Customer {

    public JointCustomer() {
        super.setCustomerType(CustomerType.JOINT);
    }

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "primary_contact_name", nullable = false)
    private String primaryContactName;

    @Column(name = "number_of_applicants", nullable = false)
    private Integer numberOfApplicants;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_joint_applicant_identities", joinColumns = @JoinColumn(name = "customer_id"))
    private List<IdentityDocument> applicantIdentities = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return accountName + " (Joint Account)";
    }
}