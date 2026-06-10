package com.digital.lending.notification.service;

import com.digital.lending.notification.dto.NotificationAuditResponseDto;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.model.NotificationAuditLog;
import com.digital.lending.notification.model.NotificationTemplate;
import com.digital.lending.notification.repository.NotificationAuditLogRepository;
import com.digital.lending.notification.repository.NotificationTemplateRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationAuditLogRepository auditLogRepository;
    private final JavaMailSender mailSender;

    @Value("${app.notification.mail.from-address:no-reply@digital-lending.com}")
    private String fromAddress;

    @Transactional
    public void processAndSendNotification(NotificationDispatchRequestDto request) {
        NotificationTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Target template framework variant not mapped: " + request.getTemplateId()));

        if (!"TRUE".equalsIgnoreCase(template.getIsActive())) {
            throw new IllegalStateException("Attempted routing through an inactive template frame: " + template.getId());
        }

        // Parse variables inside title and body strings
        String finalTitle = interpolateContent(template.getTitleTemplate(), request.getTemplateVariables());
        String finalBody = interpolateContent(template.getBodyTemplate(), request.getTemplateVariables());

        String auditId = UUID.randomUUID().toString();

        NotificationAuditLog audit = new NotificationAuditLog();
        audit.setId(auditId);
        audit.setTemplateId(template.getId());
        audit.setChannelId(template.getChannelId());
        audit.setRecipientDestination(request.getDestination());
        audit.setResolvedTitle(finalTitle);
        audit.setResolvedBody(finalBody);
        audit.setTriggeredBy(request.getActor());
        audit.setCreatedAt(LocalDateTime.now());

        try {
            // Channel Routing Mechanics Matrix switch
            switch (template.getChannelId().toUpperCase()) {
                case "EMAIL" -> dispatchEmailServiceChannel(request.getDestination(), finalTitle, finalBody);
                case "SMS" -> dispatchSmsDummyChannel(request.getDestination(), finalBody);
                case "PUSH" -> dispatchPushDummyChannel(request.getDestination(), finalTitle, finalBody);
                default -> throw new UnsupportedOperationException("Channel parsing route unknown: " + template.getChannelId());
            }
            audit.setStatus("SENT");
        } catch (Exception ex) {
            log.error("Failed notification dispatch execution run: ", ex);
            audit.setStatus("FAILED");
            audit.setErrorMessage(ex.getMessage());
        } finally {
            auditLogRepository.save(audit);
        }
    }

    private String interpolateContent(String rawTemplate, Map<String, String> variables) {
        if (rawTemplate == null) return "";
        String result = rawTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String targetPlaceholder = "{{" + entry.getKey() + "}}";
            String replacementValue = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(targetPlaceholder, replacementValue);
        }
        return result;
    }

    private void dispatchEmailServiceChannel(String recipient, String subject, String htmlContent) throws Exception {
        log.info("[SMTP GATEWAY] Preparing real mail transmission to: {}", recipient);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        // UTF-8 flag constraint guarantees that currency markers (e.g. KES, €, $) resolve correctly
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(recipient);
        helper.setSubject(subject);

        // Setting the second parameter to true tells the engine to render the text as rich HTML markup
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
        log.info("[SMTP GATEWAY] Email successfully pushed to transport relay server.");
    }

    private void dispatchSmsDummyChannel(String mobileNumber, String textMessage) {
        System.out.println("[CELLULAR GATEWAY SIMULATION] Outbound SMS Dispatch Success!");
        System.out.println("-> Target MSISDN Destination: " + mobileNumber);
        System.out.println("-> Message String Body: " + textMessage);
    }

    private void dispatchPushDummyChannel(String pushToken, String alertHeader, String alertBody) {
        System.out.println("[MOBILE PUSH GATEWAY SIMULATION] Outbound APNS/FCM Payload Broadcast Success!");
        System.out.println("-> Target Registration Token: " + pushToken);
        System.out.println("-> Alert Payload Header: [" + alertHeader + "] Body: [" + alertBody + "]");
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<NotificationAuditResponseDto> getFilteredNotificationLogs(
            String channel, String recipient, String status,
            LocalDateTime fromDate, LocalDateTime toDate, org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.jpa.domain.Specification<NotificationAuditLog> spec =
                NotificationSpecification.createSpecification(
                        channel, recipient, status, fromDate, toDate
                );

        return auditLogRepository.findAll(spec, pageable)
                .map(logItem -> new NotificationAuditResponseDto(
                        logItem.getId(),
                        logItem.getTemplateId(),
                        logItem.getChannelId(),
                        logItem.getRecipientDestination(),
                        logItem.getResolvedTitle(),
                        logItem.getResolvedBody(),
                        logItem.getStatus(),
                        logItem.getErrorMessage(),
                        logItem.getTriggeredBy(),
                        logItem.getCreatedAt()
                ));
    }
}
