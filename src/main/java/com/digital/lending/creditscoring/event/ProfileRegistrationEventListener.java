package com.digital.lending.creditscoring.event;

import com.digital.lending.creditscoring.service.CreditProfileService;
import com.digital.lending.events.InitialCreditProfileCreatedEvent;
import com.digital.lending.events.ProfileRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileRegistrationEventListener {

    private final CreditProfileService creditProfileService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void onProfileRegistered(ProfileRegisteredEvent event) {
        if (event.profileId() == null || event.profileId().isBlank()) {
            log.warn("Skipping baseline credit profile bootstrap because profileId is missing");
            return;
        }

        log.info("Initializing baseline credit profile for profile {}", event.profileId());
        creditProfileService.createBaselineProfile(event);
        eventPublisher.publishEvent(new InitialCreditProfileCreatedEvent(
                event.profileId(),
                event.profileType(),
                Instant.now()
        ));
    }
}
