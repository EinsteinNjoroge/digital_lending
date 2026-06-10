package com.digital.lending.loanaccount.event;

import com.digital.lending.events.LoanApplicationApprovedEvent;
import com.digital.lending.events.LoanApplicationRejectedEvent;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApplicationDecisionEventListener {

    private final LoanAccountManagementService accountService;

    @ApplicationModuleListener
    public void onLoanApplicationApproved(LoanApplicationApprovedEvent event) {
        log.info("Processing approved loan application event for account {}", event.loanAccountId());
        accountService.processApprovedApplication(event);
    }

    @ApplicationModuleListener
    public void onLoanApplicationRejected(LoanApplicationRejectedEvent event) {
        log.info("Processing rejected loan application event for account {}", event.loanAccountId());
        accountService.processRejectedApplication(event);
    }
}
