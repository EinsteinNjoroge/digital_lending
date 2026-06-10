package com.digital.lending.payment.event;

import com.digital.lending.events.LoanDisbursalRequestedEvent;
import com.digital.lending.payment.dto.PaymentExecutionRequestDto;
import com.digital.lending.payment.service.PaymentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanDisbursalRequestedEventListener {

    private final PaymentProcessingService paymentProcessingService;

    @Async
    @EventListener
    public void onLoanDisbursalRequested(LoanDisbursalRequestedEvent event) {
        try {
            PaymentExecutionRequestDto request = new PaymentExecutionRequestDto();
            request.setIdempotencyKey("disbursal-" + event.loanAccountId());
            request.setCategoryId("DISBURSEMENT");
            request.setProviderId(event.providerId() == null || event.providerId().isBlank() ? "INTERNAL" : event.providerId());
            request.setAccountReference(event.accountReference());
            request.setLoanAccountId(event.loanAccountId());
            request.setProfileId(event.profileId());
            request.setSenderPartyReference("LENDER_TREASURY");
            request.setReceiverPartyReference(event.destinationReference() == null || event.destinationReference().isBlank()
                    ? "PROFILE-" + event.profileId()
                    : event.destinationReference());
            request.setAmount(event.amount());
            request.setCurrency(event.currency());

            paymentProcessingService.initiateGatewayPayment(request);
        } catch (Exception ex) {
            log.error("Failed to process loan disbursal request for account {}", event.loanAccountId(), ex);
        }
    }
}
