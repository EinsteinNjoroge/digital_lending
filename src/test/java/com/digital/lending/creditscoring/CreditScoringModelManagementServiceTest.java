package com.digital.lending.creditscoring;

import com.digital.lending.creditscoring.dto.CreditScoringModelRequestDto;
import com.digital.lending.creditscoring.dto.CreditScoringModelResponseDto;
import com.digital.lending.creditscoring.model.CreditScoringModelDefinition;
import com.digital.lending.creditscoring.model.ScoringRulesPayload;
import com.digital.lending.creditscoring.repository.CreditScoringModelRepository;
import com.digital.lending.creditscoring.service.CreditScoringModelManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditScoringModelManagementServiceTest {

    @Mock
    private CreditScoringModelRepository modelRepository;

    @InjectMocks
    private CreditScoringModelManagementService managementService;

    private CreditScoringModelRequestDto sampleRequest;
    private CreditScoringModelDefinition sampleEntity;
    private final String modelId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        sampleRequest = new CreditScoringModelRequestDto();
        sampleRequest.setPartnerId("SAF_KE_01");
        sampleRequest.setCurrency("KES");
        sampleRequest.setLoanProductId("LOAN_PRODUCT_NANO");
        sampleRequest.setRulesPayload(new ScoringRulesPayload());

        sampleEntity = new CreditScoringModelDefinition();
        sampleEntity.setId(modelId);
        sampleEntity.setPartnerId("SAF_KE_01");
        sampleEntity.setCurrency("KES");
        sampleEntity.setLoanProductId("LOAN_PRODUCT_NANO");
        sampleEntity.setRulesPayload(new ScoringRulesPayload());
        sampleEntity.setActive(true);
    }

    @Test
    @DisplayName("Should save a new model cleanly when no active configuration collision exists")
    void shouldCreateNewModelSuccessfully() {
        when(modelRepository.findActiveModel("SAF_KE_01", "KES", "LOAN_PRODUCT_NANO"))
                .thenReturn(Optional.empty());
        when(modelRepository.save(any(CreditScoringModelDefinition.class))).thenReturn(sampleEntity);

        CreditScoringModelResponseDto response = managementService.createModel(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(modelId);
        assertThat(response.isActive()).isTrue();
        verify(modelRepository, times(1)).save(any(CreditScoringModelDefinition.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating a duplicate model on an active tenant coordinate")
    void shouldThrowExceptionWhenActiveModelCollisionOccurs() {
        when(modelRepository.findActiveModel("SAF_KE_01", "KES", "LOAN_PRODUCT_NANO"))
                .thenReturn(Optional.of(sampleEntity));

        assertThatThrownBy(() -> managementService.createModel(sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists for Tenant SAF_KE_01");

        verify(modelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully soft-delete an existing model profile by setting active state to false")
    void shouldPerformLogicalDeleteOnExistingModel() {
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(sampleEntity));

        managementService.deleteModelLogical(modelId);

        assertThat(sampleEntity.isActive()).isFalse();
        verify(modelRepository, times(1)).save(sampleEntity);
    }

    @Test
    @DisplayName("Should filter models index properly when passing specific partner profiles flags")
    void shouldFilterAndReturnAllModelsByCriteria() {
        when(modelRepository.findByPartnerIdAndIsActive("SAF_KE_01", true))
                .thenReturn(Collections.singletonList(sampleEntity));

        List<CreditScoringModelResponseDto> models = managementService.getAllModels("SAF_KE_01", true);

        assertThat(models).hasSize(1);
        assertThat(models.getFirst().getId()).isEqualTo(modelId);
    }
}
