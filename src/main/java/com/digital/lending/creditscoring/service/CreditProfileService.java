package com.digital.lending.creditscoring.service;

import com.digital.lending.creditscoring.enums.CreditProfileStatus;
import com.digital.lending.creditscoring.model.CreditProfile;
import com.digital.lending.creditscoring.repository.CreditProfileRepository;
import com.digital.lending.events.ProfileRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreditProfileService {

    private final CreditProfileRepository creditProfileRepository;

    @Transactional
    public CreditProfile createBaselineProfile(ProfileRegisteredEvent event) {
        return creditProfileRepository.findById(event.profileId())
                .orElseGet(() -> creditProfileRepository.save(buildBaselineProfile(event)));
    }

    @Transactional(readOnly = true)
    public Optional<CreditProfile> findByProfileId(String profileId) {
        return creditProfileRepository.findById(profileId);
    }

    private CreditProfile buildBaselineProfile(ProfileRegisteredEvent event) {
        CreditProfile creditProfile = new CreditProfile();
        creditProfile.setProfileId(event.profileId());
        creditProfile.setBaselineScore(resolveBaselineScore(event.profileType()));
        creditProfile.setIntroductoryCreditLimit(resolveIntroductoryLimit(event.profileType(), event.residenceCountry()));
        creditProfile.setCurrency(resolveCurrency(event.residenceCountry()));
        creditProfile.setStatus(CreditProfileStatus.ACTIVE);
        creditProfile.setSource("STUBBED_BASELINE");
        creditProfile.setCreatedAt(ZonedDateTime.now());
        creditProfile.setUpdatedAt(ZonedDateTime.now());
        return creditProfile;
    }

    private BigDecimal resolveBaselineScore(String profileType) {
        return switch (profileType) {
            case "CORPORATE" -> new BigDecimal("710.00");
            case "JOINT" -> new BigDecimal("680.00");
            default -> new BigDecimal("650.00");
        };
    }

    private BigDecimal resolveIntroductoryLimit(String profileType, String country) {
        BigDecimal baseLimit = switch (profileType) {
            case "CORPORATE" -> new BigDecimal("20000.00");
            case "JOINT" -> new BigDecimal("12000.00");
            default -> new BigDecimal("8000.00");
        };

        if (country != null && country.equalsIgnoreCase("KEN")) {
            return baseLimit;
        }
        return baseLimit.min(new BigDecimal("5000.00"));
    }

    private String resolveCurrency(String country) {
        if (country != null && country.equalsIgnoreCase("KEN")) {
            return "KES";
        }
        return "USD";
    }
}
