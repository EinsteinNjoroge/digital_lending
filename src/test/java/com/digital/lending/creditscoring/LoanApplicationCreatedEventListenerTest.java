package com.digital.lending.creditscoring;

import com.digital.lending.creditscoring.dto.CreditDecisionResponse;
import com.digital.lending.creditscoring.enums.CreditProfileStatus;
import com.digital.lending.creditscoring.event.LoanApplicationCreatedEventListener;
import com.digital.lending.creditscoring.model.CreditProfile;
import com.digital.lending.creditscoring.service.CreditProfileService;
import com.digital.lending.creditscoring.service.CreditScoringOrchestrationEngine;
import com.digital.lending.events.LoanApplicationApprovedEvent;
import com.digital.lending.loanaccount.repository.LoanAccountRepository;
import com.digital.lending.events.LoanApplicationCreatedEvent;
import com.digital.lending.events.LoanApplicationRejectedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationCreatedEventListenerTest {

    @Mock
    private CreditScoringOrchestrationEngine orchestrationEngine;

    @Mock
    private CreditProfileService creditProfileService;

    @Mock
    private LoanAccountRepository loanAccountRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoanApplicationCreatedEventListener listener;

    @Test
    @DisplayName("Should publish approval event when credit profile and scoring engine approve the loan application")
    void shouldPublishApprovalEventWhenApproved() {
        LoanApplicationCreatedEvent event = sampleEvent();
        when(creditProfileService.findByProfileId("PROF-1")).thenReturn(Optional.of(activeCreditProfile()));
        when(orchestrationEngine.resolveAndEvaluate(any())).thenReturn(
                new CreditDecisionResponse("123", "APPROVED", 88.0, 5000.0, Map.of("DECISION_REASON", "Approved"))
        );
        when(loanAccountRepository.sumOutstandingExposure(anyString(), anyString(), any())).thenReturn(BigDecimal.ZERO);

        listener.onLoanApplicationCreated(event);

        ArgumentCaptor<LoanApplicationApprovedEvent> captor = ArgumentCaptor.forClass(LoanApplicationApprovedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(event.loanAccountId(), captor.getValue().loanAccountId());
        assertEquals(event.profileId(), captor.getValue().profileId());
    }

    @Test
    @DisplayName("Should publish rejection event when no credit profile exists")
    void shouldPublishRejectionEventWhenCreditProfileMissing() {
        LoanApplicationCreatedEvent event = sampleEvent();
        when(creditProfileService.findByProfileId("PROF-1")).thenReturn(Optional.empty());

        listener.onLoanApplicationCreated(event);

        ArgumentCaptor<LoanApplicationRejectedEvent> captor = ArgumentCaptor.forClass(LoanApplicationRejectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("REFERRED", captor.getValue().decisionOutcome());
        verify(orchestrationEngine, never()).resolveAndEvaluate(any());
    }

    private CreditProfile activeCreditProfile() {
        CreditProfile profile = new CreditProfile();
        profile.setProfileId("PROF-1");
        profile.setBaselineScore(new BigDecimal("650.00"));
        profile.setIntroductoryCreditLimit(new BigDecimal("8000.00"));
        profile.setCurrency("KES");
        profile.setStatus(CreditProfileStatus.ACTIVE);
        profile.setSource("STUBBED_BASELINE");
        return profile;
    }

    private LoanApplicationCreatedEvent sampleEvent() {
        return new LoanApplicationCreatedEvent(
                "acc_1",
                "PROF-1",
                "LP-1",
                new BigDecimal("1000.00"),
                "SAF_KE_01",
                "KES",
                Map.of("wallet_throughput_30d", "20000"),
                "INTERNAL",
                "WALLET-PROF-1",
                "actor",
                ZonedDateTime.now()
        );
    }
}
