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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profile_individual_profile")
@DiscriminatorValue("INDIVIDUAL")
@Getter
@Setter
public class IndividualProfile extends Profile {

    public IndividualProfile() {
        super.setProfileType(ProfileType.INDIVIDUAL);
    }

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profile_individual_identities", joinColumns = @JoinColumn(name = "profile_id"))
    private List<IdentityDocument> identities = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }
}
