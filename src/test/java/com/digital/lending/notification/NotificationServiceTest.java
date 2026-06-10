package com.digital.lending.notification;

import com.digital.lending.notification.dto.NotificationAuditResponseDto;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.model.NotificationAuditLog;
import com.digital.lending.notification.model.NotificationTemplate;
import com.digital.lending.notification.repository.NotificationAuditLogRepository;
import com.digital.lending.notification.repository.NotificationTemplateRepository;
import com.digital.lending.notification.service.NotificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationAuditLogRepository auditLogRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService service;

    private NotificationDispatchRequestDto request;
    private NotificationTemplate emailTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@digital-lending.com");

        request = new NotificationDispatchRequestDto();
        request.setTemplateId("LOAN_DISBURSED_EMAIL");
        request.setDestination("client.email@example.com");
        request.setTemplateVariables(Map.of("recipientName", "John Doe", "amount", "15000.00"));
        request.setActor("LendingServiceEngine");

        emailTemplate = new NotificationTemplate();
        emailTemplate.setId("LOAN_DISBURSED_EMAIL");
        emailTemplate.setChannelId("EMAIL");
        emailTemplate.setTitleTemplate("Hello {{recipientName}}");
        emailTemplate.setBodyTemplate("Your loan amount is {{amount}}");
        emailTemplate.setIsActive("TRUE");
    }

    @Nested
    @DisplayName("processAndSendNotification")
    class ProcessAndSendNotificationTests {

        @Test
        @DisplayName("Should send email and save SENT audit log on valid active template")
        void shouldSendEmailAndSaveSentAudit() throws Exception {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
            when(templateRepository.findById("LOAN_DISBURSED_EMAIL")).thenReturn(Optional.of(emailTemplate));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(auditLogRepository.save(any(NotificationAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processAndSendNotification(request);

            verify(mailSender).send(any(MimeMessage.class));

            ArgumentCaptor<NotificationAuditLog> auditCaptor = ArgumentCaptor.forClass(NotificationAuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            assertEquals("SENT", auditCaptor.getValue().getStatus());
            assertEquals("client.email@example.com", auditCaptor.getValue().getRecipientDestination());
            assertTrue(auditCaptor.getValue().getResolvedTitle().contains("John Doe"));
            assertTrue(auditCaptor.getValue().getResolvedBody().contains("15000.00"));
        }

        @Test
        @DisplayName("Should throw when template is missing")
        void shouldThrowWhenTemplateMissing() {
            when(templateRepository.findById("LOAN_DISBURSED_EMAIL")).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.processAndSendNotification(request));

            assertEquals("Target template framework variant not mapped: LOAN_DISBURSED_EMAIL", ex.getMessage());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when template is inactive")
        void shouldThrowWhenTemplateInactive() {
            emailTemplate.setIsActive("FALSE");
            when(templateRepository.findById("LOAN_DISBURSED_EMAIL")).thenReturn(Optional.of(emailTemplate));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.processAndSendNotification(request));

            assertEquals("Attempted routing through an inactive template frame: LOAN_DISBURSED_EMAIL", ex.getMessage());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save FAILED audit when email dispatch throws exception")
        void shouldSaveFailedAuditWhenEmailDispatchFails() throws Exception {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
            when(templateRepository.findById("LOAN_DISBURSED_EMAIL")).thenReturn(Optional.of(emailTemplate));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));
            when(auditLogRepository.save(any(NotificationAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processAndSendNotification(request);

            ArgumentCaptor<NotificationAuditLog> auditCaptor = ArgumentCaptor.forClass(NotificationAuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            assertEquals("FAILED", auditCaptor.getValue().getStatus());
            assertEquals("SMTP unavailable", auditCaptor.getValue().getErrorMessage());
        }

        @Test
        @DisplayName("Should save SENT audit for SMS dispatch without email sender")
        void shouldSaveSentAuditForSmsDispatch() {
            NotificationTemplate smsTemplate = new NotificationTemplate();
            smsTemplate.setId("SMS_TEMPLATE");
            smsTemplate.setChannelId("SMS");
            smsTemplate.setTitleTemplate("Alert {{recipientName}}");
            smsTemplate.setBodyTemplate("SMS amount {{amount}}");
            smsTemplate.setIsActive("TRUE");

            when(templateRepository.findById("LOAN_DISBURSED_EMAIL")).thenReturn(Optional.of(smsTemplate));
            when(auditLogRepository.save(any(NotificationAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processAndSendNotification(request);

            verify(mailSender, never()).send(any(MimeMessage.class));
            verify(auditLogRepository).save(any(NotificationAuditLog.class));
        }
    }

    @Nested
    @DisplayName("getFilteredNotificationLogs")
    class GetFilteredNotificationLogsTests {

        @Test
        @DisplayName("Should map filtered notification audit logs to DTOs")
        void shouldMapFilteredAuditLogs() {
            NotificationAuditLog log = new NotificationAuditLog();
            log.setId("notif_001");
            log.setTemplateId("LOAN_DISBURSED_EMAIL");
            log.setChannelId("EMAIL");
            log.setRecipientDestination("client.email@example.com");
            log.setResolvedTitle("Hello John Doe");
            log.setResolvedBody("Your loan amount is 15000.00");
            log.setStatus("SENT");
            log.setTriggeredBy("LendingServiceEngine");
            log.setCreatedAt(LocalDateTime.of(2026, 6, 10, 10, 0));

            when(auditLogRepository.findAll(org.mockito.ArgumentMatchers.<Specification<NotificationAuditLog>>any(), eq(PageRequest.of(0, 10))))
                    .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));

            Page<NotificationAuditResponseDto> result = service.getFilteredNotificationLogs(
                    "EMAIL",
                    "client.email@example.com",
                    "SENT",
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59),
                    PageRequest.of(0, 10)
            );

            assertEquals(1, result.getTotalElements());
            assertEquals("notif_001", result.getContent().getFirst().id());
            assertEquals("EMAIL", result.getContent().getFirst().channelId());
        }
    }
}
