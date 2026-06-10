package com.digital.lending.creditscoring.event;

import com.digital.lending.creditscoring.dto.CreditDecisionResponse;
import com.digital.lending.creditscoring.dto.ScoringRequestDto;
import com.digital.lending.creditscoring.enums.CreditProfileStatus;
import com.digital.lending.creditscoring.model.CreditProfile;
import com.digital.lending.creditscoring.service.CreditProfileService;
import com.digital.lending.creditscoring.service.CreditScoringOrchestrationEngine;
import com.digital.lending.events.LoanApplicationApprovedEvent;
import com.digital.lending.events.LoanApplicationCreatedEvent;
import com.digital.lending.events.LoanApplicationRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApplicationCreatedEventListener {

    private static final BigDecimal MINIMUM_BASELINE_SCORE = new BigDecimal("600.00");

    private final CreditScoringOrchestrationEngine orchestrationEngine;
    private final CreditProfileService creditProfileService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void onLoanApplicationCreated(LoanApplicationCreatedEvent event) {
        Optional<CreditProfile> optionalCreditProfile = creditProfileService.findByProfileId(event.profileId());
        if (optionalCreditProfile.isEmpty()) {
            publishRejected(event, null, "REFERRED", "No operational credit profile was found for the profile.", null);
            return;
        }

        CreditProfile creditProfile = optionalCreditProfile.get();
        if (creditProfile.getStatus() != CreditProfileStatus.ACTIVE) {
            publishRejected(event, null, "REFERRED", "The credit profile is not active for underwriting.", null);
            return;
        }

        if (creditProfile.getBaselineScore().compareTo(MINIMUM_BASELINE_SCORE) < 0) {
            publishRejected(event, null, "DECLINED", "The baseline credit profile score is below the approval threshold.", creditProfile.getBaselineScore().doubleValue());
            return;
        }

        BigDecimal approvedLimit = creditProfile.getIntroductoryCreditLimit();
        Double evaluatedScore = creditProfile.getBaselineScore().doubleValue();
        String decisionId = null;

        if (hasDynamicScoringContext(event)) {
            try {
                ScoringRequestDto scoringRequest = new ScoringRequestDto();
                scoringRequest.setTransactionId(event.loanAccountId());
                scoringRequest.setProfileId(event.profileId());
                scoringRequest.setPartnerId(event.partnerId());
                scoringRequest.setCurrency(event.currency());
                scoringRequest.setLoanProductId(event.loanProductId());
                scoringRequest.setFeatures(Map.copyOf(event.scoringFeatures()));

                CreditDecisionResponse decision = orchestrationEngine.resolveAndEvaluate(scoringRequest);
                decisionId = decision.getId();
                evaluatedScore = decision.getScoreCalculated();

                if (!"APPROVED".equalsIgnoreCase(decision.getDecisionOutcome())) {
                    publishRejected(event, decisionId, decision.getDecisionOutcome(), extractReason(decision.getEvaluationTrace()), decision.getScoreCalculated());
                    return;
                }

                approvedLimit = approvedLimit.min(BigDecimal.valueOf(decision.getCreditLimitAllocated()));
            } catch (Exception ex) {
                log.error("Failed to evaluate dynamic scorecard for loan account {}", event.loanAccountId(), ex);
                publishRejected(event, decisionId, "REFERRED", ex.getMessage(), evaluatedScore);
                return;
            }
        }

        if (event.requestedPrincipal().compareTo(approvedLimit) > 0) {
            publishRejected(event, decisionId, "DECLINED", "Requested amount exceeds the approved credit limit.", evaluatedScore);
            return;
        }

        eventPublisher.publishEvent(new LoanApplicationApprovedEvent(
                event.loanAccountId(),
                decisionId,
                event.profileId(),
                event.loanProductId(),
                event.requestedPrincipal(),
                approvedLimit,
                evaluatedScore,
                event.partnerId(),
                event.currency() == null || event.currency().isBlank() ? creditProfile.getCurrency() : event.currency(),
                event.disbursementProviderId(),
                event.disbursementDestinationReference(),
                event.actor(),
                ZonedDateTime.now()
        ));
    }

    private boolean hasDynamicScoringContext(LoanApplicationCreatedEvent event) {
        return event.partnerId() != null && !event.partnerId().isBlank()
                && event.currency() != null && !event.currency().isBlank()
                && event.scoringFeatures() != null && !event.scoringFeatures().isEmpty();
    }

    private void publishRejected(
            LoanApplicationCreatedEvent event,
            String decisionId,
            String decisionOutcome,
            String rejectionReason,
            Double scoreCalculated) {

        eventPublisher.publishEvent(new LoanApplicationRejectedEvent(
                event.loanAccountId(),
                decisionId,
                event.profileId(),
                event.loanProductId(),
                event.requestedPrincipal(),
                decisionOutcome,
                rejectionReason,
                scoreCalculated,
                event.partnerId(),
                event.currency(),
                event.actor(),
                ZonedDateTime.now()
        ));
    }

    private String extractReason(Map<String, String> evaluationTrace) {
        if (evaluationTrace == null || evaluationTrace.isEmpty()) {
            return "Loan application was not approved by the credit-scoring engine.";
        }
        if (evaluationTrace.containsKey("DECISION_REASON")) {
            return evaluationTrace.get("DECISION_REASON");
        }
        if (evaluationTrace.containsKey("KO_TRIGGERED")) {
            return evaluationTrace.get("KO_TRIGGERED");
        }
        if (evaluationTrace.containsKey("ENGINE_SYSTEM_ERROR")) {
            return evaluationTrace.get("ENGINE_SYSTEM_ERROR");
        }
        return "Loan application was not approved by the credit-scoring engine.";
    }
}
