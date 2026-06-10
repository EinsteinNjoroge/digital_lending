package com.digital.lending.creditscoring.service;

import com.digital.lending.creditscoring.dto.CreditScoringModelRequestDto;
import com.digital.lending.creditscoring.dto.CreditScoringModelResponseDto;
import com.digital.lending.creditscoring.exception.RecordNotFoundException;
import com.digital.lending.creditscoring.model.CreditScoringModelDefinition;
import com.digital.lending.creditscoring.repository.CreditScoringModelRepository;
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
    public CreditScoringModelResponseDto createModel(CreditScoringModelRequestDto request) {
        modelRepository.findActiveModel(request.getPartnerId(), request.getCurrency(), request.getLoanProductId()).ifPresent(m -> {
            throw new IllegalArgumentException(String.format("An active scorecard matrix configuration model already exists for Tenant %s [%s] with loan product: %s",
                    request.getPartnerId(), request.getCurrency(), request.getLoanProductId()));
        });

        CreditScoringModelDefinition entity = new CreditScoringModelDefinition();
        entity.setId(UUID.randomUUID().toString());
        entity.setLoanProductId(request.getLoanProductId());
        entity.setPartnerId(request.getPartnerId());
        entity.setCurrency(request.getCurrency());
        entity.setRulesPayload(request.getRulesPayload());
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

        entity.setActive(false);
        entity.setUpdatedAt(ZonedDateTime.now());
        modelRepository.save(entity);
    }

    private CreditScoringModelResponseDto mapToResponseDto(CreditScoringModelDefinition entity) {
        return new CreditScoringModelResponseDto(
                entity.getId(),
                entity.getLoanProductId(),
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
