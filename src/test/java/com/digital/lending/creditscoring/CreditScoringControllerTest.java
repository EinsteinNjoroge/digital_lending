package com.digital.lending.creditscoring;

import com.digital.lending.creditscoring.controller.CreditScoringController;
import com.digital.lending.creditscoring.dto.CreditScoringModelRequestDto;
import com.digital.lending.creditscoring.dto.CreditScoringModelResponseDto;
import com.digital.lending.creditscoring.model.ScoringRulesPayload;
import com.digital.lending.creditscoring.service.CreditScoringModelManagementService;
import com.digital.lending.creditscoring.service.CreditScoringOrchestrationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreditScoringController.class,
        excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
class CreditScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreditScoringModelManagementService managementService;

    @MockitoBean
    private CreditScoringOrchestrationEngine orchestrationEngine;

    @Test
    @DisplayName("Should successfully return 200 OK along with valid model details on standard creation flows")
    void shouldReturn200AndSavedDtoOnValidPost() throws Exception {
        CreditScoringModelRequestDto request = new CreditScoringModelRequestDto();
        request.setPartnerId("SAF_KE_01");
        request.setCurrency("KES");
        request.setLoanProductId("LOAN_PRODUCT_NANO");
        request.setRulesPayload(new ScoringRulesPayload());

        CreditScoringModelResponseDto response = new CreditScoringModelResponseDto();
        response.setId(UUID.randomUUID().toString());
        response.setLoanProductId("LOAN_PRODUCT_NANO");
        response.setPartnerId("SAF_KE_01");
        response.setCurrency("KES");
        response.setActive(true);

        when(managementService.createModel(any(CreditScoringModelRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/credit-scoring/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId()))
                .andExpect(jsonPath("$.loanProductId").value("LOAN_PRODUCT_NANO"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Should capture payload validation failure and reject execution with HTTP 400 when object structure is broken")
    void shouldReturn400BadRequestWhenRulesPayloadIsMissing() throws Exception {
        CreditScoringModelRequestDto brokenRequest = new CreditScoringModelRequestDto();
        brokenRequest.setPartnerId("SAF_KE_01");
        brokenRequest.setCurrency("KES");
        brokenRequest.setLoanProductId("LOAN_PRODUCT_NANO");
        brokenRequest.setRulesPayload(null);

        mockMvc.perform(post("/api/v1/credit-scoring/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brokenRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST_PAYLOAD"))
                .andExpect(jsonPath("$.details").value(org.hamcrest.Matchers.containsString("required")));
    }

    @Test
    @DisplayName("Should cleanly resolve duplicate index conditions into HTTP 409 Conflict via GlobalExceptionHandler intercept patterns")
    void shouldBubbleUpConflictErrorOnIllegalArgumentException() throws Exception {
        CreditScoringModelRequestDto request = new CreditScoringModelRequestDto();
        request.setPartnerId("SAF_KE_01");
        request.setCurrency("KES");
        request.setLoanProductId("LOAN_PRODUCT_NANO");
        request.setRulesPayload(new ScoringRulesPayload());

        when(managementService.createModel(any(CreditScoringModelRequestDto.class)))
                .thenThrow(new IllegalArgumentException("An active scorecard matrix configuration model already exists for Tenant SAF_KE_01 [KES]"));

        mockMvc.perform(post("/api/v1/credit-scoring/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTING_CONFIGURATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @DisplayName("Should successfully list all active models under a tenant filter path configuration query string")
    void shouldFetchAndFilterActiveModelsSuccessfully() throws Exception {
        CreditScoringModelResponseDto response = new CreditScoringModelResponseDto();
        response.setId(UUID.randomUUID().toString());
        response.setLoanProductId("LOAN_PRODUCT_NANO");
        response.setActive(true);

        when(managementService.getAllModels("SAF_KE_01", true))
                .thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/v1/credit-scoring/models")
                        .param("partnerId", "SAF_KE_01")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].loanProductId").value("LOAN_PRODUCT_NANO"));
    }
}
