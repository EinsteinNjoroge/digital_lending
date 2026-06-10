package com.digital.lending.notification.event;

import com.digital.lending.events.LoanAccountSettledEvent;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.repository.ProfileContactProjectionRepository;
import com.digital.lending.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanAccountSettledEventListener {

    private static final String LOAN_SETTLED_EMAIL_TEMPLATE = "LOAN_SETTLED_EMAIL";
    private static final String LOAN_SETTLEMENT_LISTENER_ACTOR = "loan-settlement-listener";

    private final NotificationService notificationService;
    private final ProfileContactProjectionRepository profileContactProjectionRepository;

    @ApplicationModuleListener
    public void onLoanAccountSettled(LoanAccountSettledEvent event) {
        if (event.profileId() == null || event.profileId().isBlank()) {
            log.warn("Skipping settlement notification because profileId is missing");
            return;
        }

        profileContactProjectionRepository.findById(event.profileId()).ifPresentOrElse(profile -> {
            if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                log.warn("Skipping settlement notification because profile {} does not have an email address", event.profileId());
                return;
            }

            NotificationDispatchRequestDto request = new NotificationDispatchRequestDto();
            request.setTemplateId(LOAN_SETTLED_EMAIL_TEMPLATE);
            request.setDestination(profile.getEmail());
            request.setActor(LOAN_SETTLEMENT_LISTENER_ACTOR);
            request.setTemplateVariables(Map.of(
                    "recipientName", profile.getDisplayName(),
                    "accountReference", event.accountReference(),
                    "settlementDate", event.occurredAt().toLocalDate().toString()
            ));
            notificationService.processAndSendNotification(request);
        }, () -> log.warn("Skipping settlement notification because profile {} was not found", event.profileId()));
    }
}
