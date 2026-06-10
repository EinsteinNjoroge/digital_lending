package com.digital.lending.creditscoring;

import com.digital.lending.creditscoring.event.ProfileRegistrationEventListener;
import com.digital.lending.creditscoring.service.CreditProfileService;
import com.digital.lending.events.InitialCreditProfileCreatedEvent;
import com.digital.lending.events.ProfileRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileRegistrationEventListenerTest {

    @Mock
    private CreditProfileService creditProfileService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProfileRegistrationEventListener listener;

    @Test
    @DisplayName("Should create baseline credit profile and publish follow-up event")
    void shouldCreateBaselineCreditProfileAndPublishEvent() {
        ProfileRegisteredEvent event = new ProfileRegisteredEvent(
                "PROF-10029", "INDIVIDUAL", "Alex Doe", "alex@example.com", "+254700000000", "KEN", Instant.now()
        );

        listener.onProfileRegistered(event);

        verify(creditProfileService).createBaselineProfile(event);
        ArgumentCaptor<InitialCreditProfileCreatedEvent> captor = ArgumentCaptor.forClass(InitialCreditProfileCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("PROF-10029", captor.getValue().profileId());
        assertEquals("INDIVIDUAL", captor.getValue().profileType());
    }

    @Test
    @DisplayName("Should ignore registration event when profileId is missing")
    void shouldIgnoreRegistrationWhenProfileIdMissing() {
        listener.onProfileRegistered(new ProfileRegisteredEvent(
                "", "INDIVIDUAL", "Alex Doe", "alex@example.com", "+254700000000", "KEN", Instant.now()
        ));

        verifyNoInteractions(creditProfileService, eventPublisher);
    }
}
