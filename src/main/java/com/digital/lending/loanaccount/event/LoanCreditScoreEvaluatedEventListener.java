package com.digital.lending.loanaccount.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.digital.lending.loanaccount.dto.LoanAccountOpeningRequestDto;
import com.digital.lending.loanaccount.dto.LoanAccountResponseDto;
import com.digital.lending.loanaccount.dto.StatusModificationRequestDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.event.DraftLoanEvent;
import com.digital.lending.loanaccount.event.LoanApprovedStatusEvent;
import com.digital.lending.loanaccount.event.LoanCreditScoreEvaluatedEvent;
import com.digital.lending.loanaccount.exception.BusinessRuleViolationException;
import com.digital.lending.loanaccount.exception.ResourceNotFoundException;
import com.digital.lending.loanaccount.model.LoanAccount;
import com.digital.lending.loanaccount.model.LoanAccountAuditLog;
import com.digital.lending.loanaccount.repository.LoanAccountAuditLogRepository;
import com.digital.lending.loanaccount.repository.LoanAccountRepository;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;

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