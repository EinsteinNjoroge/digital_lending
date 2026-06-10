package com.digital.lending.loanaccount.event;

import com.digital.lending.events.LoanApplicationApprovedEvent;
import com.digital.lending.events.LoanApplicationRejectedEvent;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApplicationDecisionEventListener {

    private final LoanAccountManagementService accountService;

    @Async
    @EventListener
    public void onLoanApplicationApproved(LoanApplicationApprovedEvent event) {
        log.info("Processing approved loan application event for account {}", event.loanAccountId());
        accountService.processApprovedApplication(event);
    }

    @Async
    @EventListener
    public void onLoanApplicationRejected(LoanApplicationRejectedEvent event) {
        log.info("Processing rejected loan application event for account {}", event.loanAccountId());
        accountService.processRejectedApplication(event);
    }
}
