package com.digital.lending.loanproduct;

import com.digital.lending.loanproduct.controller.FamilyDefinitionManagementController;
import com.digital.lending.loanproduct.dto.FamilyDefinitionRequestDto;
import com.digital.lending.loanproduct.dto.FamilyDefinitionResponseDto;
import com.digital.lending.loanproduct.exception.BusinessRuleViolationException;
import com.digital.lending.loanproduct.exception.GlobalExceptionHandler;
import com.digital.lending.loanproduct.service.FamilyDefinitionManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {FamilyDefinitionManagementController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
public class FamilyDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FamilyDefinitionManagementService familyService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public FamilyDefinitionManagementService familyService() {
            return Mockito.mock(FamilyDefinitionManagementService.class);
        }
    }

    private FamilyDefinitionRequestDto getValidRequest() {
        FamilyDefinitionRequestDto dto = new FamilyDefinitionRequestDto();
        dto.setFamilyCode("MPESA_NANO_V1");
        dto.setDisplayName("Safaricom Consumer Micro-Lending");
        dto.setDisbursementHandlerToken("mpesaB2CDisbursementHandler");
        dto.setAccrualHandlerToken("straightLineDailyAccrualHandler");
        dto.setRepaymentHandlerToken("fifoLedgerRepaymentSettlementHandler");
        dto.setDelinquencyHandlerToken("automatedCrbSoftLockDelinquencyHandler");
        dto.setIsActive(true);
        return dto;
    }

    @Test
    @DisplayName("Happy Path: Create Family Definition returns 201 Created")
    void createFamily_HappyPath_Returns201() throws Exception {
        FamilyDefinitionRequestDto request = getValidRequest();
        FamilyDefinitionResponseDto response = new FamilyDefinitionResponseDto();
        response.setId("fdef_xyz123");
        response.setFamilyCode("MPESA_NANO_V1");
        response.setDisplayName("Safaricom Consumer Micro-Lending");
        response.setIsActive(true);
        response.setCreatedAt(ZonedDateTime.now());

        when(familyService.createFamily(any(FamilyDefinitionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/family-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("fdef_xyz123"))
                .andExpect(jsonPath("$.familyCode").value("MPESA_NANO_V1"));
    }

    @Test
    @DisplayName("Broken Path: Missing fields triggers validation 400 Bad Request")
    void createFamily_MissingMandatoryFields_Returns400() throws Exception {
        FamilyDefinitionRequestDto invalidRequest = getValidRequest();
        invalidRequest.setFamilyCode(""); // Blank out mandatory constraint boundary
        invalidRequest.setDisbursementHandlerToken(null);

        mockMvc.perform(post("/api/v1/family-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                .andExpect(jsonPath("$.details.familyCode").exists())
                .andExpect(jsonPath("$.details.disbursementHandlerToken").exists());
    }

    @Test
    @DisplayName("Broken Path: Unique Constraint collision throws 422 Unprocessable Entity")
    void createFamily_DuplicateFamilyCode_Returns422() throws Exception {
        FamilyDefinitionRequestDto request = getValidRequest();

        when(familyService.createFamily(any(FamilyDefinitionRequestDto.class)))
                .thenThrow(new BusinessRuleViolationException("An active core engine family blueprint framework already stands configured"));

        mockMvc.perform(post("/api/v1/family-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("Happy Path: Fetch all items returns 200 array list structure")
    void getAllFamilies_Returns200AndList() throws Exception {
        when(familyService.getAllFamilies()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/family-definitions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }
}