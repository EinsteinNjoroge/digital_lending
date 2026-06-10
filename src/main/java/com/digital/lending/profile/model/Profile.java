package com.digital.lending.profile.model;

import com.digital.lending.profile.enums.ProfileStatus;
import com.digital.lending.profile.enums.ProfileType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "profile_profile")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "profile_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
public abstract class Profile {

    @Id
    private String id;

    @Column(name = "profile_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ProfileType profileType;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_country_code", nullable = false)
    private String phoneCountryCode;

    @Column(name = "phone_national_number", nullable = false)
    private String phoneNationalNumber;

    @Column(name = "residence_country", nullable = false)
    private String residenceCountry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public abstract String getDisplayName();
}
