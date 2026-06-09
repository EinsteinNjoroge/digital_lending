package com.digital.lending.loanproduct;

import com.digital.lending.loanproduct.controller.LoanProductManagementController;
import com.digital.lending.loanproduct.dto.ProductConfigurationRequestDto;
import com.digital.lending.loanproduct.dto.ProductConfigurationResponseDto;
import com.digital.lending.loanproduct.exception.GlobalExceptionHandler;
import com.digital.lending.loanproduct.exception.ResourceNotFoundException;
import com.digital.lending.loanproduct.service.LoanProductManagementService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {LoanProductManagementController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
public class LoanProductConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanProductManagementService productService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public LoanProductManagementService productService() {
            return Mockito.mock(LoanProductManagementService.class);
        }
    }

    private ProductConfigurationRequestDto getValidProductRequest() {
        ProductConfigurationRequestDto dto = new ProductConfigurationRequestDto();
        dto.setProductCode("LN_MZ_CASH_NANO");
        dto.setName("Mobile Money Quick Emergency Cash Boost");
        dto.setFamilyDefinitionId("fdef_002");
        dto.setPartnerId("VODA_MZ_02");
        dto.setCurrency("MZN");
        dto.setIsActive(true);
        dto.setParameters(Map.of("interest_rate_daily", "0.0015"));
        dto.setDocumentMatrices(Map.of("UNDERWRITING_CHECKLIST", Map.of("minimum_wallet_age_months", 6)));
        return dto;
    }

    @Test
    @DisplayName("Happy Path: Complete payload creation maps 201 via standard endpoints")
    void createProduct_HappyPath_Returns201() throws Exception {
        ProductConfigurationRequestDto request = getValidProductRequest();
        ProductConfigurationResponseDto response = new ProductConfigurationResponseDto();
        response.setId("36-char-uuid-string-placeholder-index-key");
        response.setVersion(1);
        response.setProductCode("LN_MZ_CASH_NANO");

        when(productService.createProduct(any(ProductConfigurationRequestDto.class), eq("developer@ezra.co")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/loan-products")
                        .header("X-Modified-By", "developer@ezra.co")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("36-char-uuid-string-placeholder-index-key"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("Broken Path: Missing standard administrative audit headers outputs 400 Bad Request validation responses")
    void createProduct_MissingAuditHeader_Returns400() throws Exception {
        ProductConfigurationRequestDto request = getValidProductRequest();

        mockMvc.perform(post("/api/v1/loan-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ROUTING_HEADER"));
    }

    @Test
    @DisplayName("Broken Path: ISO Currency length violation throws standard 400 Payload violations mapping array blocks")
    void createProduct_InvalidCurrencyLength_Returns400() throws Exception {
        ProductConfigurationRequestDto request = getValidProductRequest();
        request.setCurrency("MZNN"); // Intentionally break 3-char validation check rule limits

        mockMvc.perform(post("/api/v1/loan-products")
                        .header("X-Modified-By", "developer@ezra.co")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST_PAYLOAD"))
                .andExpect(jsonPath("$.details.currency").exists());
    }

    @Test
    @DisplayName("Broken Path: Target Fetch via missing UUID string triggers fallback 404 handler execution block")
    void getProductById_MissingId_Returns404() throws Exception {
        String nonExistentId = "absent-uuid";
        when(productService.getProductById(nonExistentId)).thenThrow(new ResourceNotFoundException("No configuration track discovered"));

        mockMvc.perform(get("/api/v1/loan-products/" + nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}