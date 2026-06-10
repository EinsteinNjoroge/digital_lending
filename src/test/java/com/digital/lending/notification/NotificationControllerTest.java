package com.digital.lending.notification;

import com.digital.lending.notification.controller.NotificationController;
import com.digital.lending.notification.dto.NotificationAuditResponseDto;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.exception.GlobalExceptionHandler;
import com.digital.lending.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    private NotificationDispatchRequestDto validRequest() {
        NotificationDispatchRequestDto request = new NotificationDispatchRequestDto();
        request.setTemplateId("LOAN_DISBURSED_EMAIL");
        request.setDestination("client.email@example.com");
        request.setTemplateVariables(Map.of("recipientName", "John Doe", "amount", "15000.00"));
        request.setActor("LendingServiceEngine");
        return request;
    }

    @Nested
    @DisplayName("Dispatch API")
    class DispatchApiTests {

        @Test
        @DisplayName("Should accept valid notification dispatch request")
        void shouldAcceptValidDispatchRequest() throws Exception {
            doNothing().when(notificationService).processAndSendNotification(any(NotificationDispatchRequestDto.class));

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("Should reject notification dispatch when templateId is blank")
        void shouldRejectWhenTemplateIdBlank() throws Exception {
            NotificationDispatchRequestDto request = validRequest();
            request.setTemplateId("");

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.details.templateId").value("must not be blank"));
        }

        @Test
        @DisplayName("Should reject notification dispatch when variables map is null")
        void shouldRejectWhenTemplateVariablesMissing() throws Exception {
            NotificationDispatchRequestDto request = validRequest();
            request.setTemplateVariables(null);

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.details.templateVariables").exists());
        }

        @Test
        @DisplayName("Should return invalid request when template is not found")
        void shouldReturnInvalidRequestWhenTemplateMissing() throws Exception {
            doThrow(new IllegalArgumentException("Target template framework variant not mapped: LOAN_DISBURSED_EMAIL"))
                    .when(notificationService).processAndSendNotification(any(NotificationDispatchRequestDto.class));

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Should return business rule violation when template is inactive")
        void shouldReturnBusinessRuleViolationWhenTemplateInactive() throws Exception {
            doThrow(new IllegalStateException("Attempted routing through an inactive template frame: LOAN_DISBURSED_EMAIL"))
                    .when(notificationService).processAndSendNotification(any(NotificationDispatchRequestDto.class));

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        }

        @Test
        @DisplayName("Should reject malformed notification payload")
        void shouldRejectMalformedPayload() throws Exception {
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"templateId\":\"broken\","))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_PAYLOAD"));
        }
    }

    @Nested
    @DisplayName("Audit API")
    class AuditApiTests {

        @Test
        @DisplayName("Should fetch audit logs from audit endpoint")
        void shouldFetchAuditLogs() throws Exception {
            NotificationAuditResponseDto response = new NotificationAuditResponseDto(
                    "notif_001",
                    "LOAN_DISBURSED_EMAIL",
                    "EMAIL",
                    "client.email@example.com",
                    "Loan Disbursed",
                    "Your loan has been disbursed",
                    "SENT",
                    null,
                    "LendingServiceEngine",
                    LocalDateTime.of(2026, 6, 10, 10, 0)
            );

            when(notificationService.getFilteredNotificationLogs(
                    eq("EMAIL"), eq("client.email@example.com"), eq("SENT"), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/v1/notifications")
                            .param("channel", "EMAIL")
                            .param("recipient", "client.email@example.com")
                            .param("status", "SENT")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("notif_001"))
                    .andExpect(jsonPath("$.content[0].channelId").value("EMAIL"));
        }

        @Test
        @DisplayName("Should reject invalid date parameter on audit endpoint")
        void shouldRejectInvalidDateParameter() throws Exception {
            mockMvc.perform(get("/api/v1/notifications")
                            .param("fromDate", "invalid-date"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        }
    }
}
