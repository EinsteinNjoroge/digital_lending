package com.digital.lending.profile;

import com.digital.lending.events.ProfileRegisteredEvent;
import com.digital.lending.profile.dto.CreateProfileRequestDto;
import com.digital.lending.profile.dto.ProfileDto;
import com.digital.lending.profile.enums.DocumentType;
import com.digital.lending.profile.enums.ProfileStatus;
import com.digital.lending.profile.exception.DuplicateIdentityException;
import com.digital.lending.profile.exception.ProfileDomainException;
import com.digital.lending.profile.model.CorporateProfile;
import com.digital.lending.profile.model.IdentityDocument;
import com.digital.lending.profile.model.IndividualProfile;
import com.digital.lending.profile.repository.ProfileRepository;
import com.digital.lending.profile.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
class ProfileServiceTest {

    @Autowired
    private ProfileRepository repository;

    private ProfileServiceImpl profileService;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        profileService = new ProfileServiceImpl(repository, eventPublisher);
    }

    @Nested
    @DisplayName("Individual Profile Scenarios")
    class IndividualProfileTests {

        @Test
        @DisplayName("Should onboard valid individual profile and publish ProfileRegisteredEvent")
        void shouldOnboardValidIndividualProfile() {
            CreateProfileRequestDto.Individual validRequest = individualRequest(
                    "alex.njoroge@lending.global",
                    "Alex",
                    "Njoroge",
                    "12345678"
            );

            ProfileDto result = profileService.createProfile(validRequest);

            assertNotNull(result.id());
            assertEquals("INDIVIDUAL", result.profileType());
            assertEquals("ACTIVE", result.status());
            assertEquals(1, result.identities().size());

            ArgumentCaptor<ProfileRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(ProfileRegisteredEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(result.id(), eventCaptor.getValue().profileId());
            assertEquals(result.email(), eventCaptor.getValue().email());
        }

        @Test
        @DisplayName("Should fail when individual identity is duplicated")
        void shouldFailWhenIndividualIdentityDuplicated() {
            IndividualProfile seed = individualSeed("88888888");
            repository.saveAndFlush(seed);

            CreateProfileRequestDto.Individual duplicateRequest = individualRequest(
                    "second@lending.global",
                    "Second",
                    "User",
                    "88888888"
            );

            assertThrows(DuplicateIdentityException.class, () -> {
                profileService.createProfile(duplicateRequest);
                repository.flush();
            });
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("Corporate Profile Scenarios")
    class CorporateProfileTests {

        @Test
        @DisplayName("Should onboard valid corporate profile")
        void shouldOnboardValidCorporateProfile() {
            CreateProfileRequestDto.Corporate validRequest = corporateRequest("CPR-2026-XYZ89");

            ProfileDto result = profileService.createProfile(validRequest);

            assertNotNull(result.id());
            assertEquals("CORPORATE", result.profileType());
            assertEquals("ACTIVE", result.status());
            verify(eventPublisher).publishEvent(any(ProfileRegisteredEvent.class));
        }

        @Test
        @DisplayName("Should fail when corporate registration number exists")
        void shouldFailWhenCorporateRegistrationNumberExists() {
            CorporateProfile seed = corporateSeed("CPR-2026-UNIQUE");
            repository.saveAndFlush(seed);

            CreateProfileRequestDto.Corporate duplicateRequest = corporateRequest("CPR-2026-UNIQUE");

            assertThrows(DataIntegrityViolationException.class, () -> {
                profileService.createProfile(duplicateRequest);
                repository.flush();
            });
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("Joint Profile Scenarios")
    class JointProfileTests {

        @Test
        @DisplayName("Should fail when joint identity count mismatches applicant count")
        void shouldThrowExceptionOnJointIdentityCountMismatch() {
            CreateProfileRequestDto.Joint request = new CreateProfileRequestDto.Joint(
                    "joint@lending.global",
                    "+254",
                    "711222333",
                    "KEN",
                    "Alex & Mary Joint Account",
                    "Alex Njoroge",
                    3,
                    List.of(new CreateProfileRequestDto.IdentityInput(DocumentType.NATIONAL_ID, "112233"))
            );

            ProfileDomainException ex = assertThrows(ProfileDomainException.class, () -> {
                profileService.createProfile(request);
                repository.flush();
            });

            assertEquals("JOINT_IDENTITIES_MISMATCH", ex.getErrorCode());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    private CreateProfileRequestDto.Individual individualRequest(String email, String first, String last, String idNumber) {
        return new CreateProfileRequestDto.Individual(
                email,
                "+254",
                "712345678",
                "KEN",
                first,
                last,
                List.of(new CreateProfileRequestDto.IdentityInput(DocumentType.NATIONAL_ID, idNumber)),
                LocalDate.of(1996, 6, 7)
        );
    }

    private IndividualProfile individualSeed(String idNumber) {
        IndividualProfile p = new IndividualProfile();
        p.setId("SEED-IND-999");
        p.setEmail("first@lending.global");
        p.setPhoneCountryCode("+254");
        p.setPhoneNationalNumber("700000000");
        p.setResidenceCountry("KEN");
        p.setStatus(ProfileStatus.ACTIVE);
        p.setFirstName("First");
        p.setLastName("Last");
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.getIdentities().add(new IdentityDocument(DocumentType.NATIONAL_ID, idNumber));
        return p;
    }

    private CreateProfileRequestDto.Corporate corporateRequest(String regNo) {
        return new CreateProfileRequestDto.Corporate(
                "finance@devcorp.co.ke",
                "+254",
                "700111222",
                "KEN",
                "DevCorp Solutions Ltd",
                regNo,
                LocalDate.of(2020, 1, 15),
                "John Doe",
                List.of(new CreateProfileRequestDto.IdentityInput(DocumentType.NATIONAL_ID, "87654321"))
        );
    }

    private CorporateProfile corporateSeed(String regNo) {
        CorporateProfile p = new CorporateProfile();
        p.setId("SEED-CORP-888");
        p.setEmail("hq@devcorp.co.ke");
        p.setPhoneCountryCode("+254");
        p.setPhoneNationalNumber("700111111");
        p.setResidenceCountry("KEN");
        p.setStatus(ProfileStatus.ACTIVE);
        p.setCompanyName("Seed Corp");
        p.setRegistrationNumber(regNo);
        p.setIncorporationDate(LocalDate.of(2020, 1, 15));
        p.setAuthorizedSignatoryName("Jane Doe");
        p.getDirectorIdentities().add(new IdentityDocument(DocumentType.NATIONAL_ID, "55566677"));
        return p;
    }
}
