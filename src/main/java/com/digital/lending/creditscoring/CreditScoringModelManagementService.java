package com.digital.lending.creditscoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringModelManagementService {

    private final CreditScoringModelRepository modelRepository;

    @Transactional
    public CreditScoringModelResponseDto createModel(String partnerId, String currency, CreditScoringModelRequestDto request) {
        // 1. Enforce absolute uniqueness coordinates for multi-tenant configurations
        modelRepository.findActiveModel(partnerId, currency, request.getModelCode()).ifPresent(m -> {
            throw new IllegalArgumentException(String.format("An active scorecard matrix configuration model already exists for Tenant %s [%s] with code: %s",
                    partnerId, currency, request.getModelCode()));
        });

        // 2. Build entity using the nested object payload graph directly
        CreditScoringModelDefinition entity = new CreditScoringModelDefinition();
        entity.setId(UUID.randomUUID().toString());
        entity.setModelCode(request.getModelCode());
        entity.setPartnerId(partnerId);
        entity.setCurrency(currency);
        entity.setRulesPayload(request.getRulesPayload()); // Clean object reference mapping
        entity.setActive(true);
        entity.setCreatedAt(ZonedDateTime.now());
        entity.setUpdatedAt(ZonedDateTime.now());

        CreditScoringModelDefinition saved = modelRepository.save(entity);
        return mapToResponseDto(saved);
    }

    @Transactional
    public CreditScoringModelResponseDto updateModel(String id, CreditScoringModelRequestDto request) {
        CreditScoringModelDefinition entity = modelRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("No scoring model matrix discovered matching primary reference key: " + id));

        // Assign the updated rules object model graph
        entity.setRulesPayload(request.getRulesPayload());
        entity.setUpdatedAt(ZonedDateTime.now());

        return mapToResponseDto(modelRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public CreditScoringModelResponseDto getModelById(String id) {
        return modelRepository.findById(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new RecordNotFoundException("No scoring model matrix discovered matching primary reference key: " + id));
    }

    @Transactional
    public void deleteModelLogical(String id) {
        CreditScoringModelDefinition entity = modelRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("No scoring model matrix discovered matching primary reference key: " + id));

        // Enforce soft delete integrity constraints to retain complete evaluation history data trace tracking parameters
        entity.setActive(false);
        entity.setUpdatedAt(ZonedDateTime.now());
        modelRepository.save(entity);
    }

    private CreditScoringModelResponseDto mapToResponseDto(CreditScoringModelDefinition entity) {
        return new CreditScoringModelResponseDto(
                entity.getId(),
                entity.getModelCode(),
                entity.getPartnerId(),
                entity.getCurrency(),
                entity.isActive(),
                entity.getRulesPayload(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CreditScoringModelResponseDto> getAllModels(String partnerId, Boolean isActive) {
        log.info("Fetching model definitions index. Filters - Partner: {}, Active Only: {}", partnerId, isActive);

        java.util.List<CreditScoringModelDefinition> entities;

        if (partnerId != null && isActive != null) {
            entities = modelRepository.findByPartnerIdAndIsActive(partnerId, isActive);
        } else if (partnerId != null) {
            entities = modelRepository.findByPartnerId(partnerId);
        } else if (isActive != null) {
            entities = modelRepository.findByIsActive(isActive);
        } else {
            entities = modelRepository.findAll();
        }

        return entities.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
}