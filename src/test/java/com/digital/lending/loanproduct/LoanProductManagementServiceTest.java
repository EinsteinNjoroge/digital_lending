package com.digital.lending.loanproduct;

import com.digital.lending.loanproduct.dto.ProductConfigurationRequestDto;
import com.digital.lending.loanproduct.dto.ProductConfigurationResponseDto;
import com.digital.lending.loanproduct.exception.BusinessRuleViolationException;
import com.digital.lending.loanproduct.model.LoanProductConfiguration;
import com.digital.lending.loanproduct.repository.LoanProductAuditLogRepository;
import com.digital.lending.loanproduct.repository.LoanProductConfigurationRepository;
import com.digital.lending.loanproduct.service.LoanProductManagementService;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanProductManagementServiceTest {

    @Mock
    private LoanProductConfigurationRepository productRepository;

    @Mock
    private LoanProductAuditLogRepository auditLogRepository;

    @InjectMocks
    private LoanProductManagementService productService;

    private ProductConfigurationRequestDto validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ProductConfigurationRequestDto();
        validRequest.setProductCode("LN_MZ_CASH_NANO");
        validRequest.setName("Mobile Money Emergency Cash Boost");
        validRequest.setFamilyDefinitionId("fdef_002");
        validRequest.setPartnerId("VODA_MZ_02");
        validRequest.setCurrency("MZN");
        validRequest.setIsActive(true);
        validRequest.setParameters(Map.of("interest_rate_daily", "0.0015"));
        validRequest.setDocumentMatrices(Map.of("UNDERWRITING_CHECKLIST", Map.of("minimum_wallet_age_months", 6)));
    }

    @Test
    @DisplayName("Happy Path: Creating a new product handles multi-tenant checks, increments version sequences, and saves to database context logs")
    void createProduct_Success_SavesAndIncrementsVersion() {
        // GIVEN
        when(productRepository.existsByPartnerIdAndCurrencyAndProductCodeAndIsActiveTrue(
                "VODA_MZ_02", "MZN", "LN_MZ_CASH_NANO")).thenReturn(false);
        when(productRepository.findMaxVersionByProductCode("LN_MZ_CASH_NANO")).thenReturn(3);

        when(productRepository.save(any(LoanProductConfiguration.class))).thenAnswer(invocation -> {
            LoanProductConfiguration entity = invocation.getArgument(0);
            return entity; // Reflect state mutations directly
        });

        // WHEN
        ProductConfigurationResponseDto response = productService.createProduct(validRequest, "manager@ezra.co");

        // THEN
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(4, response.getVersion()); // Sequence increment assertion
        assertEquals("LN_MZ_CASH_NANO", response.getProductCode());

        verify(productRepository, times(1)).save(any(LoanProductConfiguration.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Broken Path: Attempting to overlap an existing active layout throws a Business Rule Violation Exception")
    void createProduct_ActiveOverlapCollision_ThrowsException() {
        // GIVEN
        when(productRepository.existsByPartnerIdAndCurrencyAndProductCodeAndIsActiveTrue(
                "VODA_MZ_02", "MZN", "LN_MZ_CASH_NANO")).thenReturn(true);

        // WHEN & THEN
        assertThrows(BusinessRuleViolationException.class, () -> {
            productService.createProduct(validRequest, "manager@ezra.co");
        });

        verify(productRepository, never()).save(any(LoanProductConfiguration.class));
    }

    @Test
    @DisplayName("Happy Path: Updating a product clears previous relational graphs completely to prevent orphan rows from persisting inside shared data layouts")
    void updateProduct_ClearsAndReplacesDynamicParameterGraphs() {
        // GIVEN
        String existingId = "mock-uuid-index-ref";
        LoanProductConfiguration existingEntity = new LoanProductConfiguration();
        existingEntity.setId(existingId);
        existingEntity.setParameters(new ArrayList<>());
        existingEntity.setDocumentMatrices(new ArrayList<>());
        existingEntity.addParameter("old_key", "old_value");

        when(productRepository.findById(existingId)).thenReturn(Optional.of(existingEntity));
        when(productRepository.save(any(LoanProductConfiguration.class))).thenReturn(existingEntity);

        // WHEN
        ProductConfigurationResponseDto response = productService.updateProduct(existingId, validRequest, "compliance@ezra.co");

        // THEN
        assertNotNull(response);
        assertEquals(1, response.getParameters().size());
        assertTrue(response.getParameters().containsKey("interest_rate_daily"));
        assertFalse(response.getParameters().containsKey("old_key")); // Orphan purge check validation assertion
    }

    @Test
    @DisplayName("Happy Path: Deleting a product triggers logical deactivation rather than permanent destruction inside shared systems rows parameters")
    void logicalDeleteProduct_TogglesIsActiveStateToFalse() {
        // GIVEN
        String targetId = "target-uuid-string-key";
        LoanProductConfiguration activeEntity = new LoanProductConfiguration();
        activeEntity.setId(targetId);
        activeEntity.setIsActive(true);

        when(productRepository.findById(targetId)).thenReturn(Optional.of(activeEntity));

        // WHEN
        productService.logicalDeleteProduct(targetId, "auditor@ezra.co");

        // THEN
        assertFalse(activeEntity.getIsActive()); // Core state alteration trace flag validation check
        verify(productRepository, times(1)).save(activeEntity);
    }
}