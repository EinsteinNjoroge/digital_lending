package com.digital.lending.loanproduct.service;

import com.digital.lending.events.LoanProductConfigurationChangedEvent;
import com.digital.lending.loanproduct.dto.ProductConfigurationRequestDto;
import com.digital.lending.loanproduct.dto.ProductConfigurationResponseDto;
import com.digital.lending.loanproduct.exception.BusinessRuleViolationException;
import com.digital.lending.loanproduct.exception.ResourceNotFoundException;
import com.digital.lending.loanproduct.model.LoanProductConfiguration;
import com.digital.lending.loanproduct.model.LoanProductConfigurationAuditLog;
import com.digital.lending.loanproduct.repository.LoanProductAuditLogRepository;
import com.digital.lending.loanproduct.repository.LoanProductConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanProductManagementService {

    private static final long DEFAULT_REPAYMENT_DUE_DAYS = 30L;

    private final LoanProductConfigurationRepository productRepository;
    private final LoanProductAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductConfigurationResponseDto createProduct(ProductConfigurationRequestDto request, String modifiedBy) {
        if (productRepository.existsByPartnerIdAndCurrencyAndProductCodeAndIsActiveTrue(request.getPartnerId(), request.getCurrency(), request.getProductCode())) {
            throw new BusinessRuleViolationException(String.format("An active configuration track already exists for Partner: %s, Currency: %s matching Code: %s", request.getPartnerId(), request.getCurrency(), request.getProductCode()));
        }

        LoanProductConfiguration config = new LoanProductConfiguration();
        config.setId(UUID.randomUUID().toString());
        config.setProductCode(request.getProductCode());
        config.setName(request.getName());
        config.setFamilyDefinitionId(request.getFamilyDefinitionId());
        config.setPartnerId(request.getPartnerId());
        config.setCurrency(request.getCurrency());
        config.setIsActive(request.getIsActive());

        int nextVersion = productRepository.findMaxVersionByProductCode(request.getProductCode()) + 1;
        config.setVersion(nextVersion);

        if (request.getParameters() != null) {
            request.getParameters().forEach(config::addParameter);
        }
        if (request.getDocumentMatrices() != null) {
            request.getDocumentMatrices().forEach(config::addMatrix);
        }

        LoanProductConfiguration savedEntity = persist(config);
        logAuditAction(savedEntity.getId(), "CREATE", modifiedBy, Map.of("version_allocated", nextVersion));
        publishProjectionEvent(savedEntity);
        return convertToResponseDto(savedEntity);
    }

    @Transactional
    public ProductConfigurationResponseDto updateProduct(String id, ProductConfigurationRequestDto request, String modifiedBy) {
        LoanProductConfiguration existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No configuration track discovered matching key index parameter: " + id));

        existingProduct.setName(request.getName());
        existingProduct.setIsActive(request.getIsActive());
        existingProduct.setUpdatedAt(ZonedDateTime.now());

        existingProduct.getParameters().clear();
        if (request.getParameters() != null) {
            request.getParameters().forEach(existingProduct::addParameter);
        }

        existingProduct.getDocumentMatrices().clear();
        if (request.getDocumentMatrices() != null) {
            request.getDocumentMatrices().forEach(existingProduct::addMatrix);
        }

        LoanProductConfiguration updatedEntity = persist(existingProduct);
        logAuditAction(id, "UPDATE", modifiedBy, Map.of("updated_fields", "parameter_graphs_and_metadata"));
        publishProjectionEvent(updatedEntity);
        return convertToResponseDto(updatedEntity);
    }

    @Transactional(readOnly = true)
    public ProductConfigurationResponseDto getProductById(String id) {
        return productRepository.findById(id).map(this::convertToResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("No configuration track discovered matching key index parameter: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProductConfigurationResponseDto> getFilteredProducts(String partnerId, String currency, Boolean isActive) {
        return productRepository.findByFilters(partnerId, currency, isActive).stream()
                .map(this::convertToResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public void logicalDeleteProduct(String id, String modifiedBy) {
        LoanProductConfiguration product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No configuration track discovered matching key index parameter: " + id));
        product.setIsActive(false);
        product.setUpdatedAt(ZonedDateTime.now());
        LoanProductConfiguration savedProduct = persist(product);
        logAuditAction(id, "DELETE", modifiedBy, Map.of("action", "logical_deactivation"));
        publishProjectionEvent(savedProduct);
    }

    private LoanProductConfiguration persist(LoanProductConfiguration entity) {
        LoanProductConfiguration savedEntity = productRepository.save(entity);
        return savedEntity != null ? savedEntity : entity;
    }

    private ProductConfigurationResponseDto convertToResponseDto(LoanProductConfiguration entity) {
        ProductConfigurationResponseDto dto = new ProductConfigurationResponseDto();
        dto.setId(entity.getId());
        dto.setVersion(entity.getVersion());
        dto.setProductCode(entity.getProductCode());
        dto.setName(entity.getName());
        dto.setFamilyDefinitionId(entity.getFamilyDefinitionId());
        dto.setPartnerId(entity.getPartnerId());
        dto.setCurrency(entity.getCurrency());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        dto.setParameters(entity.getParameters().stream().collect(Collectors.toMap(p -> p.getParameterKey(), p -> p.getParameterValue(), (a, b) -> a)));
        Map<String, Object> matrixMap = new HashMap<>();
        entity.getDocumentMatrices().forEach(m -> matrixMap.put(m.getMatrixType(), m.getPayload()));
        dto.setDocumentMatrices(matrixMap);
        return dto;
    }

    private void publishProjectionEvent(LoanProductConfiguration entity) {
        eventPublisher.publishEvent(new LoanProductConfigurationChangedEvent(
                entity.getId(),
                entity.getProductCode(),
                entity.getPartnerId(),
                entity.getCurrency(),
                Boolean.TRUE.equals(entity.getIsActive()),
                resolveRepaymentDueDays(entity),
                entity.getUpdatedAt() == null ? ZonedDateTime.now() : entity.getUpdatedAt()
        ));
    }

    private long resolveRepaymentDueDays(LoanProductConfiguration entity) {
        Map<String, String> parameters = entity.getParameters().stream().collect(Collectors.toMap(
                parameter -> parameter.getParameterKey().toLowerCase(),
                parameter -> parameter.getParameterValue(),
                (first, second) -> first
        ));

        return firstNumericValue(parameters,
                "repayment_due_days",
                "max_tenor_days",
                "review_cycle_days",
                "season_length_days",
                "merchant_settlement_delay_days")
                .orElse(DEFAULT_REPAYMENT_DUE_DAYS);
    }

    private java.util.Optional<Long> firstNumericValue(Map<String, String> parameters, String... keys) {
        for (String key : keys) {
            String value = parameters.get(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return java.util.Optional.of(Long.parseLong(value));
            } catch (NumberFormatException ex) {
                log.warn("Ignoring non-numeric loan product parameter {}={} while resolving repayment due days", key, value);
            }
        }
        return java.util.Optional.empty();
    }

    private void logAuditAction(String productId, String actionType, String user, Map<String, Object> context) {
        try {
            LoanProductConfigurationAuditLog logEntry = new LoanProductConfigurationAuditLog();
            logEntry.setProductId(productId);
            logEntry.setActionType(actionType);
            logEntry.setModifiedBy(user);
            logEntry.setChangedAttributes(context);
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Unable to execute transaction footprint registration audit update sequences safely: ", e);
        }
    }
}
