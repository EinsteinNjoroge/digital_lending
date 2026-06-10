package com.digital.lending.payment.event;

import com.digital.lending.events.ProfileRegisteredEvent;
import com.digital.lending.payment.model.PaymentParty;
import com.digital.lending.payment.repository.PaymentPartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileRegisteredEventListener {

    private final PaymentPartyRepository paymentPartyRepository;

    @Async
    @Transactional
    @EventListener
    public void onProfileRegistered(ProfileRegisteredEvent event) {
        if (event.profileId() == null || event.profileId().isBlank()) {
            log.warn("Skipping payment party bootstrap because profileId is missing");
            return;
        }

        paymentPartyRepository.findByPartyReference(event.profileId())
                .ifPresentOrElse(existing -> {
                    existing.setDisplayName(resolveDisplayName(event));
                    existing.setPartyType("PROFILE");
                    existing.setSourceModule("PROFILE");
                    existing.setUpdatedAt(LocalDateTime.now());
                    paymentPartyRepository.save(existing);
                }, () -> paymentPartyRepository.save(newParty(event)));
    }

    private PaymentParty newParty(ProfileRegisteredEvent event) {
        LocalDateTime now = LocalDateTime.now();
        PaymentParty party = new PaymentParty();
        party.setId("party_" + event.profileId());
        party.setPartyReference(event.profileId());
        party.setDisplayName(resolveDisplayName(event));
        party.setPartyType("PROFILE");
        party.setSourceModule("PROFILE");
        party.setCreatedAt(now);
        party.setUpdatedAt(now);
        return party;
    }

    private String resolveDisplayName(ProfileRegisteredEvent event) {
        if (event.displayName() != null && !event.displayName().isBlank()) {
            return event.displayName();
        }
        if (event.email() != null && !event.email().isBlank()) {
            return event.email();
        }
        return "Profile " + event.profileId();
    }
}
