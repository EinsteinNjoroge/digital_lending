package com.digital.lending.profile.model;

import com.digital.lending.profile.enums.ProfileType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profile_joint_profile")
@DiscriminatorValue("JOINT")
@Getter
@Setter
public class JointProfile extends Profile {

    public JointProfile() {
        super.setProfileType(ProfileType.JOINT);
    }

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "primary_contact_name", nullable = false)
    private String primaryContactName;

    @Column(name = "number_of_applicants", nullable = false)
    private Integer numberOfApplicants;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profile_joint_applicant_identities", joinColumns = @JoinColumn(name = "profile_id"))
    private List<IdentityDocument> applicantIdentities = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return accountName + " (Joint Account)";
    }
}
