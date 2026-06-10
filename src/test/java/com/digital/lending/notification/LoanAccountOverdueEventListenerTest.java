package com.digital.lending.notification;

import com.digital.lending.events.LoanAccountOverdueEvent;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.event.LoanAccountOverdueEventListener;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanAccountOverdueEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProfileContactProjectionRepository profileContactProjectionRepository;

    @InjectMocks
    private LoanAccountOverdueEventListener listener;

    @Test
    @DisplayName("Should dispatch overdue notification with servicing status details")
    void shouldDispatchOverdueNotification() {
        when(profileContactProjectionRepository.findById("prof_burgundy_001")).thenReturn(Optional.of(profile("prof_burgundy_001", "Burgundy Evolution", "burgundy.evolution@gmail.com")));

        listener.onLoanAccountOverdue(new LoanAccountOverdueEvent(
                "acc_burgundy_001",
                "prof_burgundy_001",
                "LN-BURG-0001",
                new BigDecimal("3500.00"),
                "KES",
                5,
                "WATCH",
                ZonedDateTime.parse("2026-06-10T12:30:00Z")
        ));

        ArgumentCaptor<NotificationDispatchRequestDto> captor = ArgumentCaptor.forClass(NotificationDispatchRequestDto.class);
        verify(notificationService).processAndSendNotification(captor.capture());
        assertEquals("MISSED_PAYMENT_EMAIL", captor.getValue().getTemplateId());
        assertEquals("burgundy.evolution@gmail.com", captor.getValue().getDestination());
        assertEquals("WATCH", captor.getValue().getTemplateVariables().get("performanceStatus"));
        assertEquals("5", captor.getValue().getTemplateVariables().get("daysPastDue"));
    }

    @Test
    @DisplayName("Should ignore overdue event when profile is missing")
    void shouldIgnoreOverdueEventWhenProfileMissing() {
        when(profileContactProjectionRepository.findById("prof_missing")).thenReturn(Optional.empty());

        listener.onLoanAccountOverdue(new LoanAccountOverdueEvent(
                "acc_missing",
                "prof_missing",
                "LN-MISSING-1",
                new BigDecimal("1000.00"),
                "KES",
                3,
                "WATCH",
                ZonedDateTime.parse("2026-06-10T12:30:00Z")
        ));

        verify(profileContactProjectionRepository).findById("prof_missing");
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
