package com.digital.lending.notification;

import com.digital.lending.events.PaymentEvent;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.event.PaymentNotificationEventListener;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private PaymentNotificationEventListener listener;

    @Test
    @DisplayName("Should dispatch disbursement email notification for completed payment events")
    void shouldDispatchDisbursementNotification() {
        when(profileService.findProfileById("PROF-1")).thenReturn(Optional.of(new ProfileDto(
                "PROF-1", "INDIVIDUAL", "alex@example.com", "+254", "700000000", "KEN", "ACTIVE", "Alex Doe", List.of(), Instant.now()
        )));

        listener.onPaymentCompleted(new PaymentEvent(
                "tx_1", "PROF-1", "LN-2026-1001", "DISBURSEMENT", "INTERNAL", "COMPLETED",
                new BigDecimal("1500.00"), "KES", "INTREF001", LocalDateTime.of(2026, 6, 10, 11, 0)
        ));

        ArgumentCaptor<NotificationDispatchRequestDto> captor = ArgumentCaptor.forClass(NotificationDispatchRequestDto.class);
        verify(notificationService).processAndSendNotification(captor.capture());
        assertEquals("LOAN_DISBURSED_EMAIL", captor.getValue().getTemplateId());
        assertEquals("alex@example.com", captor.getValue().getDestination());
    }

    @Test
    @DisplayName("Should ignore incomplete or unsupported payment events")
    void shouldIgnoreUnsupportedEvents() {
        listener.onPaymentCompleted(new PaymentEvent(
                "tx_1", null, "LN-2026-1001", "REVERSAL", "INTERNAL", "FAILED",
                new BigDecimal("1500.00"), "KES", "INTREF001", LocalDateTime.of(2026, 6, 10, 11, 0)
        ));

        verifyNoInteractions(notificationService, profileService);
    }
}
