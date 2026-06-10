package com.digital.lending.payment.event;

import com.digital.lending.events.ProfileRegisteredEvent;
import com.digital.lending.events.ProfileUpdatedEvent;
import com.digital.lending.payment.model.PaymentParty;
import com.digital.lending.payment.repository.PaymentPartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileRegisteredEventListener {

    private final PaymentPartyRepository paymentPartyRepository;

    @ApplicationModuleListener
    public void onProfileRegistered(ProfileRegisteredEvent event) {
        upsertParty(event.profileId(), event.displayName(), event.email(), event.occurredAt());
    }

    @ApplicationModuleListener
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        upsertParty(event.profileId(), event.displayName(), event.email(), event.occurredAt());
    }

    private void upsertParty(String profileId, String displayName, String email, Instant occurredAt) {
        if (profileId == null || profileId.isBlank()) {
            log.warn("Skipping payment party bootstrap because profileId is missing");
            return;
        }

        LocalDateTime updatedAt = occurredAt == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC);

        paymentPartyRepository.findByPartyReference(profileId)
                .ifPresentOrElse(existing -> {
                    existing.setDisplayName(resolveDisplayName(profileId, displayName, email));
                    existing.setPartyType("PROFILE");
                    existing.setSourceModule("PROFILE");
                    existing.setUpdatedAt(updatedAt);
                    paymentPartyRepository.save(existing);
                }, () -> paymentPartyRepository.save(newParty(profileId, displayName, email, updatedAt)));
    }

    private PaymentParty newParty(String profileId, String displayName, String email, LocalDateTime now) {
        PaymentParty party = new PaymentParty();
        party.setId("party_" + profileId);
        party.setPartyReference(profileId);
        party.setDisplayName(resolveDisplayName(profileId, displayName, email));
        party.setPartyType("PROFILE");
        party.setSourceModule("PROFILE");
        party.setCreatedAt(now);
        party.setUpdatedAt(now);
        return party;
    }

    private String resolveDisplayName(String profileId, String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (email != null && !email.isBlank()) {
            return email;
        }
        return "Profile " + profileId;
    }
}
