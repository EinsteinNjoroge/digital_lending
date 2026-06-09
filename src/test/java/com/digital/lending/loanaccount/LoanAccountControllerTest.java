package com.digital.lending.loanaccount;

import com.digital.lending.loanaccount.controller.LoanAccountController;
import com.digital.lending.loanaccount.dto.LoanAccountOpeningRequestDto;
import com.digital.lending.loanaccount.dto.LoanAccountResponseDto;
import com.digital.lending.loanaccount.dto.StatusModificationRequestDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.exception.AccountsModuleExceptionHandler;
import com.digital.lending.loanaccount.exception.BusinessRuleViolationException;
import com.digital.lending.loanaccount.exception.ResourceNotFoundException;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {LoanAccountController.class, AccountsModuleExceptionHandler.class})
@WebMvcTest(controllers = LoanAccountController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
class LoanAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanAccountManagementService loanAccountManagementService;

    private static final String BASE_API_URL = "/api/v1/loan-accounts";
    private static final String HEADER_MODIFIED_BY = "X-Modified-By";
    private static final String TEST_USER = "postman_automated_runner";

    private LoanAccountOpeningRequestDto createValidRequest() {
        LoanAccountOpeningRequestDto dto = new LoanAccountOpeningRequestDto();
        dto.setProfileId("CUST-405037");
        dto.setLoanProductId("f186e626-c8b0-4e0c-bc1a-592b68f275ca");
        dto.setIdempotencyKey("idem_tx_pmfhi4ox");
        dto.setInitialPrincipal(new BigDecimal("5000.00"));
        dto.setParentLoanAccountId(null);
        return dto;
    }

    @Nested
    @DisplayName("Scenario 1: Input Payload Structure Validation (HTTP 400)")
    class PayloadValidationTests {

        @Test
        @DisplayName("Should return 400 Bad Request when mandatory validation constraints fail")
        void shouldReturn400WhenPayloadViolatesConstraints() throws Exception {
            LoanAccountOpeningRequestDto breakingRequest = createValidRequest();
            breakingRequest.setProfileId("");
            breakingRequest.setInitialPrincipal(BigDecimal.ZERO);

            mockMvc.perform(post(BASE_API_URL)
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(breakingRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.message").value("Input payload validation failed. Check parameter compliance constraints."))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.details.profileId").exists())
                    .andExpect(jsonPath("$.details.initialPrincipal").exists());
        }
    }

    @Nested
    @DisplayName("Scenario 2: Business Policy & Eligibility Execution Rules (HTTP 422)")
    class BusinessRuleTests {

        @Test
        @DisplayName("Should return 422 when customer violates underlying active loan exposure rule")
        void shouldReturn422WhenOverlappingRiskProfileIsTriggered() throws Exception {
            LoanAccountOpeningRequestDto validRequest = createValidRequest();
            String exceptionMessage = "Profile already holds an unresolved active loan position for this product code.";

            // Stub the exact method call inside LoanAccountManagementService
            when(loanAccountManagementService.provisionNewAccount(any(LoanAccountOpeningRequestDto.class), anyString()))
                    .thenThrow(new BusinessRuleViolationException(exceptionMessage));

            mockMvc.perform(post(BASE_API_URL)
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                    .andExpect(jsonPath("$.message").value(exceptionMessage))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 422 when altering state of an unissued or denied loan reference account")
        void shouldReturn422WhenModifyingPerformanceStatusOnUnissuedAccount() throws Exception {
            String targetAccountId = "acc_unissued_9912";
            StatusModificationRequestDto modificationRequest = new StatusModificationRequestDto();
            modificationRequest.setTargetStatus(PerformanceStatus.WATCH);

            when(loanAccountManagementService.modifyPerformanceStatus(anyString(), any(StatusModificationRequestDto.class), anyString()))
                    .thenThrow(new BusinessRuleViolationException("Cannot change the performance status of an unissued or denied loan account record line."));

            mockMvc.perform(patch(BASE_API_URL + "/" + targetAccountId + "/performance-status")
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(modificationRequest)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                    .andExpect(jsonPath("$.message").value("Cannot change the performance status of an unissued or denied loan account record line."));
        }
    }

    @Nested
    @DisplayName("Scenario 3: Resource Lookup Dependencies (HTTP 404)")
    class NotFoundTests {

        @Test
        @DisplayName("Should return 404 when target loan account record is missing during modifications")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            String missingAccountId = "acc_missing_0000";
            StatusModificationRequestDto modificationRequest = new StatusModificationRequestDto();
            modificationRequest.setTargetStatus(PerformanceStatus.WATCH);

            when(loanAccountManagementService.modifyPerformanceStatus(anyString(), any(StatusModificationRequestDto.class), anyString()))
                    .thenThrow(new ResourceNotFoundException("Target loan account reference identifier not discovered."));

            mockMvc.perform(patch(BASE_API_URL + "/" + missingAccountId + "/performance-status")
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(modificationRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Target loan account reference identifier not discovered."));
        }
    }

    @Nested
    @DisplayName("Scenario 4: Relational Database Integrity & Uniqueness (HTTP 409)")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should return 409 Conflict when concurrent unique active product constraint breaches at db boundary")
        void shouldReturn409WhenActiveProductConstraintBreached() throws Exception {
            LoanAccountOpeningRequestDto validRequest = createValidRequest();
            String dbInternalMessage = "Exception executing batch statement; constraint [uk_active_product_per_profile]";

            when(loanAccountManagementService.provisionNewAccount(any(), anyString()))
                    .thenThrow(new DataIntegrityViolationException(dbInternalMessage));

            mockMvc.perform(post(BASE_API_URL)
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT_ERROR"))
                    .andExpect(jsonPath("$.message").value("Concurrent exposure fallback blocked by database tracking logic. Profile already has an active loan of this product type."));
        }

        @Test
        @DisplayName("Should return 409 Conflict when unique transaction idempotency key tracker hits")
        void shouldReturn409WhenIdempotencyConstraintBreached() throws Exception {
            LoanAccountOpeningRequestDto validRequest = createValidRequest();
            String dbInternalMessage = "Exception executing batch statement; constraint [uk_loan_account_accounts_idempotency]";

            when(loanAccountManagementService.provisionNewAccount(any(), anyString()))
                    .thenThrow(new DataIntegrityViolationException(dbInternalMessage));

            mockMvc.perform(post(BASE_API_URL)
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT_ERROR"))
                    .andExpect(jsonPath("$.message").value("Duplicate transaction processing blocked by unique idempotency constraint tracking safety keys."));
        }
    }

    @Nested
    @DisplayName("Scenario 5: Global Fallback Handler Plane (HTTP 500)")
    class SystemicGlobalFailureTests {

        @Test
        @DisplayName("Should return 500 Internal Error when encountering unexpected backend crashes")
        void shouldReturn500WhenUnhandledExceptionOccurs() throws Exception {
            LoanAccountOpeningRequestDto validRequest = createValidRequest();

            when(loanAccountManagementService.provisionNewAccount(any(), anyString()))
                    .thenThrow(new NullPointerException("Fatal pointer offset error calculating memory buffers"));

            mockMvc.perform(post(BASE_API_URL)
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_FAULT"))
                    .andExpect(jsonPath("$.message").value("A critical system execution failure occurred. Engineering team notified."));
        }
    }

    @Nested
    @DisplayName("Scenario 6: Successful Provisioning Happy Path Execution")
    class HappyPathTests {

        @Test
        @DisplayName("Should return 200/201 and valid response object when request passes downstream footprint constraints")
        void shouldReturnSuccessPayloadWhenRequestComplies() throws Exception {
            LoanAccountOpeningRequestDto validRequest = createValidRequest();

            LoanAccountResponseDto mockResponse = new LoanAccountResponseDto();
            mockResponse.setId("acc_b706abda-78ab-4bfe-a61f-165987bba796");
            mockResponse.setProfileId("CUST-405037");
            mockResponse.setLoanProductId(validRequest.getLoanProductId());
            mockResponse.setIdempotencyKey("idem_tx_pmfhi4ox");
            mockResponse.setInitialPrincipal(new BigDecimal("5000.00"));
            mockResponse.setIssuanceStatus(IssuanceStatus.DRAFT);

            when(loanAccountManagementService.provisionNewAccount(any(LoanAccountOpeningRequestDto.class), anyString()))
                    .thenReturn(mockResponse);

            mockMvc.perform(post(BASE_API_URL)
                            .header(HEADER_MODIFIED_BY, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("acc_b706abda-78ab-4bfe-a61f-165987bba796"))
                    .andExpect(jsonPath("$.profileId").value("CUST-405037"))
                    .andExpect(jsonPath("$.initialPrincipal").value(5000.00))
                    .andExpect(jsonPath("$.issuanceStatus").value("DRAFT"));
        }
    }
}