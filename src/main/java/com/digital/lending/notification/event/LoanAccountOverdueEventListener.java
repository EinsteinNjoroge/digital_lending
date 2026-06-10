package com.digital.lending.notification.event;

import com.digital.lending.events.LoanAccountOverdueEvent;
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
public class LoanAccountOverdueEventListener {

    private static final String MISSED_PAYMENT_EMAIL_TEMPLATE = "MISSED_PAYMENT_EMAIL";
    private static final String OVERDUE_NOTIFICATION_ACTOR = "loan-servicing-listener";

    private final NotificationService notificationService;
    private final ProfileContactProjectionRepository profileContactProjectionRepository;

    @ApplicationModuleListener
    public void onLoanAccountOverdue(LoanAccountOverdueEvent event) {
        if (event.profileId() == null || event.profileId().isBlank()) {
            log.warn("Skipping overdue notification because profileId is missing");
            return;
        }

        profileContactProjectionRepository.findById(event.profileId()).ifPresentOrElse(profile -> {
            if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                log.warn("Skipping overdue notification because profile {} does not have an email address", event.profileId());
                return;
            }

            NotificationDispatchRequestDto request = new NotificationDispatchRequestDto();
            request.setTemplateId(MISSED_PAYMENT_EMAIL_TEMPLATE);
            request.setDestination(profile.getEmail());
            request.setActor(OVERDUE_NOTIFICATION_ACTOR);
            request.setTemplateVariables(Map.of(
                    "recipientName", profile.getDisplayName(),
                    "amount", event.outstandingPrincipal().toPlainString(),
                    "currency", event.currency(),
                    "accountReference", event.accountReference(),
                    "dueDate", event.occurredAt().minusDays(event.daysPastDue()).toLocalDate().toString(),
                    "performanceStatus", event.performanceStatus(),
                    "daysPastDue", String.valueOf(event.daysPastDue())
            ));
            notificationService.processAndSendNotification(request);
        }, () -> log.warn("Skipping overdue notification because profile {} was not found", event.profileId()));
    }
}
