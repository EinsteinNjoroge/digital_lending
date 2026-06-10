package com.digital.lending.payment;

import com.digital.lending.events.ProfileRegisteredEvent;
import com.digital.lending.payment.event.ProfileRegisteredEventListener;
import com.digital.lending.payment.model.PaymentParty;
import com.digital.lending.payment.repository.PaymentPartyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileRegisteredEventListenerTest {

    @Mock
    private PaymentPartyRepository paymentPartyRepository;

    @InjectMocks
    private ProfileRegisteredEventListener listener;

    @Test
    @DisplayName("Should create payment party on first profile registration")
    void shouldCreatePaymentPartyOnFirstProfileRegistration() {
        ProfileRegisteredEvent event = new ProfileRegisteredEvent(
                "PROF-10029", "INDIVIDUAL", "Alex Doe", "alex@example.com", "+254700000000", "KEN", Instant.now()
        );
        when(paymentPartyRepository.findByPartyReference("PROF-10029")).thenReturn(Optional.empty());
        when(paymentPartyRepository.save(any(PaymentParty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onProfileRegistered(event);

        ArgumentCaptor<PaymentParty> captor = ArgumentCaptor.forClass(PaymentParty.class);
        verify(paymentPartyRepository).save(captor.capture());
        assertEquals("party_PROF-10029", captor.getValue().getId());
        assertEquals("PROF-10029", captor.getValue().getPartyReference());
        assertEquals("Alex Doe", captor.getValue().getDisplayName());
        assertEquals("PROFILE", captor.getValue().getPartyType());
    }

    @Test
    @DisplayName("Should update an existing payment party on duplicate registration")
    void shouldUpdateExistingPaymentParty() {
        PaymentParty existing = new PaymentParty();
        existing.setId("party_PROF-10029");
        existing.setPartyReference("PROF-10029");
        existing.setDisplayName("Old Name");

        when(paymentPartyRepository.findByPartyReference("PROF-10029")).thenReturn(Optional.of(existing));
        when(paymentPartyRepository.save(any(PaymentParty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onProfileRegistered(new ProfileRegisteredEvent(
                "PROF-10029", "INDIVIDUAL", "New Name", "alex@example.com", "+254700000000", "KEN", Instant.now()
        ));

        assertEquals("New Name", existing.getDisplayName());
        assertEquals("PROFILE", existing.getPartyType());
        verify(paymentPartyRepository).save(existing);
    }

    @Test
    @DisplayName("Should ignore registration event when profileId is missing")
    void shouldIgnoreRegistrationWhenProfileIdMissing() {
        listener.onProfileRegistered(new ProfileRegisteredEvent(
                "", "INDIVIDUAL", "Alex Doe", "alex@example.com", "+254700000000", "KEN", Instant.now()
        ));

        verifyNoInteractions(paymentPartyRepository);
    }
}
