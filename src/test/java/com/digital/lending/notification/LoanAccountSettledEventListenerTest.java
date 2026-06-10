package com.digital.lending.notification;

import com.digital.lending.events.LoanAccountSettledEvent;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.event.LoanAccountSettledEventListener;
import com.digital.lending.notification.service.NotificationService;
import com.digital.lending.profile.dto.ProfileDto;
import com.digital.lending.profile.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanAccountSettledEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private LoanAccountSettledEventListener listener;

    @Test
    @DisplayName("Should dispatch settlement email notification when profile is available")
    void shouldDispatchSettlementNotification() {
        when(profileService.findProfileById("PROF-1")).thenReturn(Optional.of(new ProfileDto(
                "PROF-1", "INDIVIDUAL", "alex@example.com", "+254", "700000000", "KEN", "ACTIVE", "Alex Doe", List.of(), Instant.now()
        )));

        listener.onLoanAccountSettled(new LoanAccountSettledEvent(
                "acc_1", "PROF-1", "LN-2026-1001", ZonedDateTime.parse("2026-06-10T12:30:00Z")
        ));

        ArgumentCaptor<NotificationDispatchRequestDto> captor = ArgumentCaptor.forClass(NotificationDispatchRequestDto.class);
        verify(notificationService).processAndSendNotification(captor.capture());
        assertEquals("LOAN_SETTLED_EMAIL", captor.getValue().getTemplateId());
        assertEquals("alex@example.com", captor.getValue().getDestination());
        assertEquals("LN-2026-1001", captor.getValue().getTemplateVariables().get("accountReference"));
    }

    @Test
    @DisplayName("Should ignore settlement event when profileId is missing")
    void shouldIgnoreSettlementEventWhenProfileIdMissing() {
        listener.onLoanAccountSettled(new LoanAccountSettledEvent(
                "acc_1", "", "LN-2026-1001", ZonedDateTime.parse("2026-06-10T12:30:00Z")
        ));

        verifyNoInteractions(profileService, notificationService);
    }

    @Test
    @DisplayName("Should ignore settlement event when profile has no email")
    void shouldIgnoreSettlementEventWhenProfileHasNoEmail() {
        when(profileService.findProfileById("PROF-1")).thenReturn(Optional.of(new ProfileDto(
                "PROF-1", "INDIVIDUAL", "", "+254", "700000000", "KEN", "ACTIVE", "Alex Doe", List.of(), Instant.now()
        )));

        listener.onLoanAccountSettled(new LoanAccountSettledEvent(
                "acc_1", "PROF-1", "LN-2026-1001", ZonedDateTime.parse("2026-06-10T12:30:00Z")
        ));

        verify(profileService).findProfileById("PROF-1");
        verifyNoInteractions(notificationService);
    }
}
