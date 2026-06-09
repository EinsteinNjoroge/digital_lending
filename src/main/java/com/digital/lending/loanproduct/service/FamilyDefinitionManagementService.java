package com.digital.lending.loanproduct.service;

import com.digital.lending.loanproduct.dto.FamilyDefinitionRequestDto;
import com.digital.lending.loanproduct.dto.FamilyDefinitionResponseDto;
import com.digital.lending.loanproduct.exception.BusinessRuleViolationException;
import com.digital.lending.loanproduct.exception.ResourceNotFoundException;
import com.digital.lending.loanproduct.model.LoanProductFamilyDefinition;
import com.digital.lending.loanproduct.repository.LoanProductFamilyDefinitionRepository;
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
public class FamilyDefinitionManagementService {

    private final LoanProductFamilyDefinitionRepository familyRepository;

    @Transactional
    public FamilyDefinitionResponseDto createFamily(FamilyDefinitionRequestDto request) {
        if (familyRepository.existsByFamilyCodeAndIsActiveTrue(request.getFamilyCode())) {
            throw new BusinessRuleViolationException(String.format(
                    "An active core engine family blueprint framework already stands configured matching code: %s",
                    request.getFamilyCode()));
        }

        LoanProductFamilyDefinition family = new LoanProductFamilyDefinition();
        // Standard structural layout generation prefixing the ID domain signature
        family.setId("fdef_" + UUID.randomUUID().toString().substring(0, 8));
        family.setFamilyCode(request.getFamilyCode().toUpperCase().trim());
        hydrateEntity(family, request);

        LoanProductFamilyDefinition savedEntity = familyRepository.save(family);
        log.info("Successfully provisioned new core loan family structural handler trace path: {}", savedEntity.getId());
        return convertToResponseDto(savedEntity);
    }

    @Transactional
    public FamilyDefinitionResponseDto updateFamily(String id, FamilyDefinitionRequestDto request) {
        LoanProductFamilyDefinition family = familyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No engine family path registered matching reference: " + id));

        hydrateEntity(family, request);
        family.setUpdatedAt(ZonedDateTime.now());

        return convertToResponseDto(familyRepository.save(family));
    }

    @Transactional(readOnly = true)
    public FamilyDefinitionResponseDto getFamilyById(String id) {
        return familyRepository.findById(id)
                .map(this::convertToResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("No engine family path registered matching reference: " + id));
    }

    @Transactional(readOnly = true)
    public List<FamilyDefinitionResponseDto> getAllFamilies() {
        return familyRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void logicalDeleteFamily(String id) {
        LoanProductFamilyDefinition family = familyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No engine family path registered matching reference: " + id));
        family.setIsActive(false);
        family.setUpdatedAt(ZonedDateTime.now());
        familyRepository.save(family);
        log.warn("Logical decommissioning executed across target calculation family token index: {}", id);
    }

    private void hydrateEntity(LoanProductFamilyDefinition entity, FamilyDefinitionRequestDto request) {
        entity.setDisplayName(request.getDisplayName());
        entity.setDisbursementHandlerToken(request.getDisbursementHandlerToken());
        entity.setAccrualHandlerToken(request.getAccrualHandlerToken());
        entity.setRepaymentHandlerToken(request.getRepaymentHandlerToken());
        entity.setDelinquencyHandlerToken(request.getDelinquencyHandlerToken());
        entity.setIsActive(request.getIsActive());
    }

    private FamilyDefinitionResponseDto convertToResponseDto(LoanProductFamilyDefinition entity) {
        FamilyDefinitionResponseDto dto = new FamilyDefinitionResponseDto();
        dto.setId(entity.getId());
        dto.setFamilyCode(entity.getFamilyCode());
        dto.setDisplayName(entity.getDisplayName());
        dto.setDisbursementHandlerToken(entity.getDisbursementHandlerToken());
        dto.setAccrualHandlerToken(entity.getAccrualHandlerToken());
        dto.setRepaymentHandlerToken(entity.getRepaymentHandlerToken());
        dto.setDelinquencyHandlerToken(entity.getDelinquencyHandlerToken());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}