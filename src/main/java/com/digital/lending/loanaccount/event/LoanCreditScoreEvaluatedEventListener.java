package com.digital.lending.loanaccount.event;

import com.digital.lending.loanaccount.service.LoanAccountManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanCreditScoreEvaluatedEventListener {

    private final LoanAccountManagementService accountService;

    @Async // Frees up thread execution planes back to core frameworks efficiently
    @EventListener
    public void onCreditScoreEvaluated(LoanCreditScoreEvaluatedEvent event) {
        log.info("Intercepted inbound asynchronous underwriting outcome callback trace map execution for Account: {}", event.getLoanAccountId());
        try {
            accountService.processUnderwritingOutcome(event);
        } catch (Exception ex) {
            log.error("Systemic tracking failure caught routing evaluations context for ID: {}", event.getLoanAccountId(), ex);
        }
    }
}