package com.digital.lending.notification.event;

import com.digital.lending.events.PaymentEvent;
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
public class PaymentNotificationEventListener {

    private final NotificationService notificationService;
    private final ProfileService profileService;

    @Async
    @EventListener
    public void onPaymentCompleted(PaymentEvent event) {
        if (!"COMPLETED".equalsIgnoreCase(event.statusId())) {
            return;
        }

        String templateId = resolveTemplateId(event.categoryId());
        if (templateId == null) {
            return;
        }

        if (event.profileId() == null || event.profileId().isBlank()) {
            log.warn("Skipping payment notification because no profileId was supplied for transaction {}", event.transactionId());
            return;
        }

        profileService.findProfileById(event.profileId()).ifPresentOrElse(profile -> {
            if (profile.email() == null || profile.email().isBlank()) {
                log.warn("Skipping payment notification because profile {} does not have an email address", event.profileId());
                return;
            }

            NotificationDispatchRequestDto request = new NotificationDispatchRequestDto();
            request.setTemplateId(templateId);
            request.setDestination(profile.email());
            request.setActor("payment-event-listener");
            request.setTemplateVariables(Map.of(
                    "recipientName", profile.displayName(),
                    "amount", event.amount().toPlainString(),
                    "currency", event.currency(),
                    "accountReference", event.accountReference(),
                    "valueDate", event.timestamp().toLocalDate().toString()
            ));
            notificationService.processAndSendNotification(request);
        }, () -> log.warn("Skipping payment notification because profile {} was not found", event.profileId()));
    }

    private String resolveTemplateId(String categoryId) {
        if ("DISBURSEMENT".equalsIgnoreCase(categoryId)) {
            return "LOAN_DISBURSED_EMAIL";
        }
        if ("REPAYMENT".equalsIgnoreCase(categoryId)) {
            return "REPAYMENT_RECEIVED_EMAIL";
        }
        return null;
    }
}
