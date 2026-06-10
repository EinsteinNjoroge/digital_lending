package com.digital.lending.profile.service.impl;

import com.digital.lending.events.ProfileRegisteredEvent;
import com.digital.lending.profile.dto.CreateProfileRequest;
import com.digital.lending.profile.dto.IdentityDto;
import com.digital.lending.profile.dto.ProfileDto;
import com.digital.lending.profile.dto.UpdateProfileRequest;
import com.digital.lending.profile.enums.ProfileStatus;
import com.digital.lending.profile.exception.DuplicateIdentityException;
import com.digital.lending.profile.exception.IdentityRequiredException;
import com.digital.lending.profile.exception.ProfileDomainException;
import com.digital.lending.profile.model.CorporateProfile;
import com.digital.lending.profile.model.IdentityDocument;
import com.digital.lending.profile.model.IndividualProfile;
import com.digital.lending.profile.model.Profile;
import com.digital.lending.profile.model.JointProfile;
import com.digital.lending.profile.repository.ProfileRepository;
import com.digital.lending.profile.service.ProfileService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public ProfileServiceImpl(ProfileRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ProfileDto createProfile(CreateProfileRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateIdentityException("email", request.email());
        }

        Profile profile = switch (request) {
            case CreateProfileRequest.Individual ind -> {
                if (ind.identities() == null || ind.identities().isEmpty()) {
                    throw new IdentityRequiredException();
                }

                IndividualProfile individual = new IndividualProfile();
                individual.setFirstName(ind.firstName());
                individual.setLastName(ind.lastName());
                individual.setDateOfBirth(ind.dateOfBirth());

                List<IdentityDocument> docs = ind.identities().stream()
                        .map(i -> {
                            validateGlobalUniqueness(i.documentNumber());
                            return new IdentityDocument(i.documentType(), i.documentNumber());
                        }).toList();
                individual.setIdentities(docs);
                yield individual;
            }
            case CreateProfileRequest.Corporate corp -> {
                if (corp.directorIdentities() == null || corp.directorIdentities().isEmpty()) {
                    throw new ProfileDomainException("DIRECTORS_IDENTITY_REQUIRED", "Corporate registrations require identity documents for all directors.") {};
                }

                CorporateProfile corporate = new CorporateProfile();
                corporate.setCompanyName(corp.companyName());
                corporate.setRegistrationNumber(corp.registrationNumber());
                corporate.setIncorporationDate(corp.incorporationDate());
                corporate.setAuthorizedSignatoryName(corp.authorizedSignatoryName());

                List<IdentityDocument> docs = corp.directorIdentities().stream()
                        .map(d -> {
                            validateGlobalUniqueness(d.documentNumber());
                            return new IdentityDocument(d.documentType(), d.documentNumber());
                        }).toList();
                corporate.setDirectorIdentities(docs);
                yield corporate;
            }
            case CreateProfileRequest.Joint jointReq -> {
                if (jointReq.applicantIdentities() == null || jointReq.applicantIdentities().size() != jointReq.numberOfApplicants()) {
                    throw new ProfileDomainException("JOINT_IDENTITIES_MISMATCH", "The provided document list count must match the total number of applicants.") {};
                }

                JointProfile joint = new JointProfile();
                joint.setAccountName(jointReq.accountName());
                joint.setPrimaryContactName(jointReq.primaryContactName());
                joint.setNumberOfApplicants(jointReq.numberOfApplicants());

                List<IdentityDocument> docs = jointReq.applicantIdentities().stream()
                        .map(a -> {
                            validateGlobalUniqueness(a.documentNumber());
                            return new IdentityDocument(a.documentType(), a.documentNumber());
                        }).toList();
                joint.setApplicantIdentities(docs);
                yield joint;
            }
            default -> throw new IllegalArgumentException("Unknown profile payload type");
        };

        profile.setId(UUID.randomUUID().toString());
        profile.setEmail(request.email());
        profile.setPhoneCountryCode(request.phoneCountryCode());
        profile.setPhoneNationalNumber(request.phoneNationalNumber());
        profile.setResidenceCountry(request.residenceCountry().toUpperCase());
        profile.setStatus(ProfileStatus.ACTIVE);

        Profile savedProfile = repository.save(profile);
        eventPublisher.publishEvent(new ProfileRegisteredEvent(
                savedProfile.getId(),
                savedProfile.getProfileType().name(),
                savedProfile.getDisplayName(),
                savedProfile.getEmail(),
                savedProfile.getPhoneCountryCode() + savedProfile.getPhoneNationalNumber(),
                savedProfile.getResidenceCountry(),
                Instant.now()
        ));

        return mapToDto(savedProfile);
    }

    private void validateGlobalUniqueness(String docNumber) {
        if (repository.existsInIndividualIdentities(docNumber) ||
                repository.existsInCorporateIdentities(docNumber) ||
                repository.existsInJointIdentities(docNumber)) {
            throw new DuplicateIdentityException("identity_document", docNumber);
        }
    }

    @Override
    public Optional<ProfileDto> findProfileById(String id) {
        return repository.findById(id).map(this::mapToDto);
    }

    @Override
    public List<ProfileDto> findAllProfiles() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProfileDto updateProfile(String id, UpdateProfileRequest request) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found with ID: " + id));

        if (!profile.getEmail().equalsIgnoreCase(request.email()) && repository.existsByEmail(request.email())) {
            throw new DuplicateIdentityException("email", request.email());
        }

        profile.setEmail(request.email());
        profile.setPhoneCountryCode(request.phoneCountryCode());
        profile.setPhoneNationalNumber(request.phoneNationalNumber());
        profile.setResidenceCountry(request.residenceCountry().toUpperCase());

        return mapToDto(repository.save(profile));
    }

    @Override
    @Transactional
    public void deleteProfile(String id) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found with ID: " + id));

        profile.setStatus(ProfileStatus.DELETED);
        repository.save(profile);
    }

    @Override
    public boolean isProfileActive(String id) {
        return repository.findById(id)
                .map(profile -> profile.getStatus() == ProfileStatus.ACTIVE)
                .orElse(false);
    }

    private ProfileDto mapToDto(Profile profile) {
        List<IdentityDocument> sourceDocs = switch (profile) {
            case IndividualProfile ind -> ind.getIdentities();
            case CorporateProfile corp -> corp.getDirectorIdentities();
            case JointProfile joint -> joint.getApplicantIdentities();
            default -> List.of();
        };

        List<IdentityDto> mappedIdentities = sourceDocs.stream()
                .map(d -> new IdentityDto(d.getDocumentType(), d.getDocumentNumber()))
                .toList();

        return new ProfileDto(
                profile.getId(),
                profile.getProfileType().name(),
                profile.getEmail(),
                profile.getPhoneCountryCode(),
                profile.getPhoneNationalNumber(),
                profile.getResidenceCountry(),
                profile.getStatus().name(),
                profile.getDisplayName(),
                mappedIdentities,
                profile.getCreatedAt()
        );
    }
}
