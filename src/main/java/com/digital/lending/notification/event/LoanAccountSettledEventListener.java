package com.digital.lending.notification.event;

import com.digital.lending.events.LoanAccountSettledEvent;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.service.NotificationService;
import com.digital.lending.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanAccountSettledEventListener {

    private final NotificationService notificationService;
    private final ProfileService profileService;

    @Async
    @EventListener
    public void onLoanAccountSettled(LoanAccountSettledEvent event) {
        if (event.profileId() == null || event.profileId().isBlank()) {
            log.warn("Skipping settlement notification because profileId is missing");
            return;
        }

        profileService.findProfileById(event.profileId()).ifPresentOrElse(profile -> {
            if (profile.email() == null || profile.email().isBlank()) {
                log.warn("Skipping settlement notification because profile {} does not have an email address", event.profileId());
                return;
            }

            NotificationDispatchRequestDto request = new NotificationDispatchRequestDto();
            request.setTemplateId("LOAN_SETTLED_EMAIL");
            request.setDestination(profile.email());
            request.setActor("loan-settlement-listener");
            request.setTemplateVariables(Map.of(
                    "recipientName", profile.displayName(),
                    "accountReference", event.accountReference(),
                    "settlementDate", event.occurredAt().toLocalDate().toString()
            ));
            notificationService.processAndSendNotification(request);
        }, () -> log.warn("Skipping settlement notification because profile {} was not found", event.profileId()));
    }
}
