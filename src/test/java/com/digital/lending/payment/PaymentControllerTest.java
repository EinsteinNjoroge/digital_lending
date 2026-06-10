package com.digital.lending.payment;

import com.digital.lending.payment.controller.PaymentCategoryController;
import com.digital.lending.payment.controller.PaymentController;
import com.digital.lending.payment.dto.PaymentCategoryRequestDto;
import com.digital.lending.payment.dto.PaymentCategoryResponseDto;
import com.digital.lending.payment.dto.PaymentExecutionRequestDto;
import com.digital.lending.payment.dto.PaymentProviderCallbackRequestDto;
import com.digital.lending.payment.dto.PaymentResponseDto;
import com.digital.lending.payment.exception.GlobalExceptionHandler;
import com.digital.lending.payment.service.PaymentCategoryService;
import com.digital.lending.payment.service.PaymentProcessingService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {PaymentController.class, PaymentCategoryController.class},
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentProcessingService paymentProcessingService;

    @MockitoBean
    private PaymentCategoryService paymentCategoryService;

    private PaymentExecutionRequestDto validPaymentRequest() {
        PaymentExecutionRequestDto request = new PaymentExecutionRequestDto();
        request.setIdempotencyKey("idem-key-88192-332");
        request.setCategoryId("REPAYMENT");
        request.setProviderId("MPESA");
        request.setAccountReference("LN-2026-99102");
        request.setSenderPartyReference("PART-CUST-10029");
        request.setReceiverPartyReference("PART-CO-DISBURSE-01");
        request.setAmount(new BigDecimal("7500.00"));
        request.setCurrency("KES");
        return request;
    }

    private PaymentResponseDto validPaymentResponse() {
        return new PaymentResponseDto(
                "tx_99201882",
                "REPAYMENT",
                "MPESA",
                "COMPLETED",
                "LN-2026-99102",
                new BigDecimal("7500.00"),
                "KES",
                "MPESAREF1234ABCD",
                LocalDateTime.of(2026, 6, 10, 10, 30)
        );
    }

    private PaymentProviderCallbackRequestDto validCallbackRequest() {
        PaymentProviderCallbackRequestDto request = new PaymentProviderCallbackRequestDto();
        request.setInternalTransactionId("tx_99201882");
        request.setProviderTransactionId("MPESATX1234ABCD");
        request.setExternalReferenceNumber("MPESAREF1234ABCD");
        request.setOutcomeStatus("COMPLETED");
        request.setAccountReference("LN-2026-99102");
        request.setProfileId("PROF-10029");
        request.setCategoryId("REPAYMENT");
        request.setAmount(new BigDecimal("7500.00"));
        request.setCurrency("KES");
        request.setRawPayload("{\"provider\":\"MPESA\",\"status\":\"COMPLETED\"}");
        request.setCallbackTimestamp(LocalDateTime.of(2026, 6, 10, 10, 30));
        return request;
    }

    @Nested
    @DisplayName("Payments API")
    class PaymentsApiTests {

        @Test
        @DisplayName("Should create payment successfully on valid request")
        void shouldCreatePaymentSuccessfully() throws Exception {
            when(paymentProcessingService.registerAndProcessPayment(any(PaymentExecutionRequestDto.class)))
                    .thenReturn(validPaymentResponse());

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPaymentRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("tx_99201882"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.provider").value("MPESA"));
        }

        @Test
        @DisplayName("Should reject payment request when idempotency key is blank")
        void shouldRejectWhenIdempotencyKeyIsBlank() throws Exception {
            PaymentExecutionRequestDto request = validPaymentRequest();
            request.setIdempotencyKey("");

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.details.idempotencyKey").value("must not be blank"));
        }

        @Test
        @DisplayName("Should reject payment request when amount is null")
        void shouldRejectWhenAmountIsNull() throws Exception {
            PaymentExecutionRequestDto request = validPaymentRequest();
            request.setAmount(null);

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.details.amount").exists());
        }

        @Test
        @DisplayName("Should reject payment request when amount is not positive")
        void shouldRejectWhenAmountIsNotPositive() throws Exception {
            PaymentExecutionRequestDto request = validPaymentRequest();
            request.setAmount(BigDecimal.ZERO);

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.details.amount").exists());
        }

        @Test
        @DisplayName("Should return standardized bad request when payload is malformed")
        void shouldReturnBadRequestOnMalformedJson() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotencyKey\": \"broken\","))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_PAYLOAD"));
        }

        @Test
        @DisplayName("Should return invalid request when service rejects unknown provider")
        void shouldReturnInvalidRequestWhenProviderMissing() throws Exception {
            when(paymentProcessingService.registerAndProcessPayment(any(PaymentExecutionRequestDto.class)))
                    .thenThrow(new IllegalArgumentException("Payment provider not found: MPESA"));

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPaymentRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Payment provider not found: MPESA"));
        }

        @Test
        @DisplayName("Should accept provider callback successfully")
        void shouldAcceptProviderCallbackSuccessfully() throws Exception {
            when(paymentProcessingService.processProviderCallback(eq("MPESA"), any(PaymentProviderCallbackRequestDto.class)))
                    .thenReturn(validPaymentResponse());

            mockMvc.perform(post("/api/v1/payments/providers/MPESA/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCallbackRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("tx_99201882"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should reject provider callback when required fields are missing")
        void shouldRejectInvalidProviderCallbackPayload() throws Exception {
            PaymentProviderCallbackRequestDto request = validCallbackRequest();
            request.setOutcomeStatus("");
            request.setAmount(null);

            mockMvc.perform(post("/api/v1/payments/providers/MPESA/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.details.outcomeStatus").exists())
                    .andExpect(jsonPath("$.details.amount").exists());
        }

        @Test
        @DisplayName("Should return invalid request when provider callback is rejected by service")
        void shouldReturnInvalidRequestForProviderCallbackServiceError() throws Exception {
            when(paymentProcessingService.processProviderCallback(eq("MPESA"), any(PaymentProviderCallbackRequestDto.class)))
                    .thenThrow(new IllegalArgumentException("Unsupported callback outcome status: UNKNOWN"));

            mockMvc.perform(post("/api/v1/payments/providers/MPESA/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCallbackRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Unsupported callback outcome status: UNKNOWN"));
        }

        @Test
        @DisplayName("Should list filtered payments successfully")
        void shouldListFilteredPaymentsSuccessfully() throws Exception {
            when(paymentProcessingService.getFilteredPayments(
                    any(), any(), eq("PART-CUST-10029"), eq("LN-2026-99102"), eq("MPESA"), eq("KES"), any()))
                    .thenReturn(new PageImpl<>(List.of(validPaymentResponse()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/v1/payments")
                            .param("profileId", "PART-CUST-10029")
                            .param("accountReference", "LN-2026-99102")
                            .param("providerId", "MPESA")
                            .param("currency", "KES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("tx_99201882"))
                    .andExpect(jsonPath("$.content[0].provider").value("MPESA"));
        }

        @Test
        @DisplayName("Should reject invalid date parameter on payment listing")
        void shouldRejectInvalidDateParameter() throws Exception {
            mockMvc.perform(get("/api/v1/payments")
                            .param("fromDate", "not-a-date"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        }
    }

    @Nested
    @DisplayName("Payment Categories API")
    class PaymentCategoryApiTests {

        @Test
        @DisplayName("Should create category successfully")
        void shouldCreateCategorySuccessfully() throws Exception {
            PaymentCategoryRequestDto request = new PaymentCategoryRequestDto("REPAYMENT", "Loan Repayment", "Repayment category");
            PaymentCategoryResponseDto response = new PaymentCategoryResponseDto("REPAYMENT", "Loan Repayment", "Repayment category", Instant.now());

            when(paymentCategoryService.create(any(PaymentCategoryRequestDto.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/payment/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("REPAYMENT"))
                    .andExpect(jsonPath("$.name").value("Loan Repayment"));
        }

        @Test
        @DisplayName("Should reject category creation when id is blank")
        void shouldRejectCategoryCreationWhenIdBlank() throws Exception {
            PaymentCategoryRequestDto request = new PaymentCategoryRequestDto("", "Loan Repayment", "Repayment category");

            mockMvc.perform(post("/api/v1/payment/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                    .andExpect(jsonPath("$.details.id").value("must not be blank"));
        }

        @Test
        @DisplayName("Should fetch category by id")
        void shouldFetchCategoryById() throws Exception {
            PaymentCategoryResponseDto response = new PaymentCategoryResponseDto("REPAYMENT", "Loan Repayment", "Repayment category", Instant.now());
            when(paymentCategoryService.get("REPAYMENT")).thenReturn(response);

            mockMvc.perform(get("/api/v1/payment/categories/REPAYMENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("REPAYMENT"));
        }

        @Test
        @DisplayName("Should return not found when category does not exist")
        void shouldReturnNotFoundWhenCategoryMissing() throws Exception {
            when(paymentCategoryService.get("UNKNOWN")).thenThrow(new RuntimeException("Category not found"));

            mockMvc.perform(get("/api/v1/payment/categories/UNKNOWN"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should list all categories")
        void shouldListAllCategories() throws Exception {
            when(paymentCategoryService.getAll()).thenReturn(List.of(
                    new PaymentCategoryResponseDto("REPAYMENT", "Loan Repayment", "Repayment category", Instant.now())
            ));

            mockMvc.perform(get("/api/v1/payment/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("REPAYMENT"));
        }

        @Test
        @DisplayName("Should update category successfully")
        void shouldUpdateCategorySuccessfully() throws Exception {
            PaymentCategoryRequestDto request = new PaymentCategoryRequestDto("REPAYMENT", "Loan Repayment Updated", "Updated repayment category");
            PaymentCategoryResponseDto response = new PaymentCategoryResponseDto("REPAYMENT", "Loan Repayment Updated", "Updated repayment category", Instant.now());

            when(paymentCategoryService.update(eq("REPAYMENT"), any(PaymentCategoryRequestDto.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/payment/categories/REPAYMENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Loan Repayment Updated"));
        }

        @Test
        @DisplayName("Should delete category successfully")
        void shouldDeleteCategorySuccessfully() throws Exception {
            doNothing().when(paymentCategoryService).delete("REPAYMENT");

            mockMvc.perform(delete("/api/v1/payment/categories/REPAYMENT"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return conflict on category duplicate collision")
        void shouldReturnConflictOnCategoryDuplicateCollision() throws Exception {
            PaymentCategoryRequestDto request = new PaymentCategoryRequestDto("REPAYMENT", "Loan Repayment", "Repayment category");
            doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"))
                    .when(paymentCategoryService).create(any(PaymentCategoryRequestDto.class));

            mockMvc.perform(post("/api/v1/payment/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("DATA_INTEGRITY_COLLISION"));
        }
    }
}
