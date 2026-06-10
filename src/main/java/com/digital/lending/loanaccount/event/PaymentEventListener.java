package com.digital.lending.loanaccount.event;

import com.digital.lending.events.PaymentEvent;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final LoanAccountManagementService loanAccountManagementService;

    @Async
    @EventListener
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Processing payment event {} for account reference {}", event.categoryId(), event.accountReference());
        loanAccountManagementService.processPaymentEvent(event);
    }
}
