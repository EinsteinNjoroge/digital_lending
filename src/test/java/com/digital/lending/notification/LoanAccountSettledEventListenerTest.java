package com.digital.lending.notification;

import com.digital.lending.events.LoanAccountSettledEvent;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.event.LoanAccountSettledEventListener;
import com.digital.lending.notification.model.ProfileContactProjection;
import com.digital.lending.notification.repository.ProfileContactProjectionRepository;
import com.digital.lending.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanAccountSettledEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProfileContactProjectionRepository profileContactProjectionRepository;

    @InjectMocks
    private LoanAccountSettledEventListener listener;

    @Test
    @DisplayName("Should dispatch settlement email notification when profile is available")
    void shouldDispatchSettlementNotification() {
        when(profileContactProjectionRepository.findById("PROF-1")).thenReturn(Optional.of(profile("PROF-1", "Alex Doe", "alex@example.com")));

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

        verifyNoInteractions(profileContactProjectionRepository, notificationService);
    }

    @Test
    @DisplayName("Should ignore settlement event when profile has no email")
    void shouldIgnoreSettlementEventWhenProfileHasNoEmail() {
        when(profileContactProjectionRepository.findById("PROF-1")).thenReturn(Optional.of(profile("PROF-1", "Alex Doe", "")));

        listener.onLoanAccountSettled(new LoanAccountSettledEvent(
                "acc_1", "PROF-1", "LN-2026-1001", ZonedDateTime.parse("2026-06-10T12:30:00Z")
        ));

        verify(profileContactProjectionRepository).findById("PROF-1");
        verifyNoInteractions(notificationService);
    }

    private ProfileContactProjection profile(String id, String displayName, String email) {
        ProfileContactProjection projection = new ProfileContactProjection();
        projection.setProfileId(id);
        projection.setDisplayName(displayName);
        projection.setEmail(email);
        projection.setStatus("ACTIVE");
        projection.setUpdatedAt(Instant.now());
        return projection;
    }
}
