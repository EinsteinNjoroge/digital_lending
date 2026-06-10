package com.digital.lending.payment.service;

import com.digital.lending.events.PaymentEvent;
import com.digital.lending.events.ProviderPayoutCompletedEvent;
import com.digital.lending.payment.dto.PaymentExecutionRequestDto;
import com.digital.lending.payment.dto.PaymentProviderCallbackRequestDto;
import com.digital.lending.payment.dto.PaymentResponseDto;
import com.digital.lending.payment.gateway.PaymentGatewayClient;
import com.digital.lending.payment.gateway.PaymentGatewayInitiationResult;
import com.digital.lending.payment.gateway.PaymentGatewayRequest;
import com.digital.lending.payment.model.PaymentParty;
import com.digital.lending.payment.model.PaymentProviderMetadata;
import com.digital.lending.payment.model.PaymentTransaction;
import com.digital.lending.payment.repository.PaymentCategoryRepository;
import com.digital.lending.payment.repository.PaymentPartyRepository;
import com.digital.lending.payment.repository.PaymentProviderMetadataRepository;
import com.digital.lending.payment.repository.PaymentProviderRepository;
import com.digital.lending.payment.repository.PaymentTransactionRepository;
import com.digital.lending.payment.repository.TransactionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_REVERSED = "REVERSED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String CATEGORY_DISBURSEMENT = "DISBURSEMENT";
    private static final String CATEGORY_REPAYMENT = "REPAYMENT";
    private static final String EXTERNAL_PAYER_REFERENCE = "EXTERNAL_PAYER";
    private static final String LENDER_TREASURY_REFERENCE = "LENDER_TREASURY";
    private static final String DYNAMIC_PARTY_TYPE = "EXTERNAL";
    private static final String DYNAMIC_PARTY_SOURCE_MODULE = "PAYMENT";
    private static final String CACHED_IDEMPOTENCY_LIMIT_REFERENCE = "CACHED_IDEMPOTENCY_LIMIT";

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentPartyRepository partyRepository;
    private final PaymentProviderRepository providerRepository;
    private final PaymentCategoryRepository categoryRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final PaymentProviderMetadataRepository metadataRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponseDto registerAndProcessPayment(PaymentExecutionRequestDto request) {
        return transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(this::mapToResponseDto)
                .orElseGet(() -> executeImmediateTransactionFlow(request));
    }

    @Transactional
    public PaymentResponseDto initiateGatewayPayment(PaymentExecutionRequestDto request) {
        return transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(this::mapToResponseDto)
                .orElseGet(() -> executeGatewayInitiationFlow(request));
    }

    @Transactional
    public PaymentResponseDto processProviderCallback(String providerId, PaymentProviderCallbackRequestDto request) {
        String normalizedProviderId = providerId.toUpperCase();
        Optional<PaymentTransaction> existingTransaction = resolveExistingTransaction(request);

        if (existingTransaction.isEmpty()) {
            return createRepaymentFromCallback(normalizedProviderId, request);
        }

        PaymentTransaction transaction = existingTransaction.get();
        if (!transaction.getProviderId().equalsIgnoreCase(normalizedProviderId)) {
            throw new IllegalArgumentException("Provider callback does not match the stored transaction provider.");
        }

        if (isTerminalStatus(transaction.getStatusId())) {
            return mapToResponseDto(transaction);
        }

        String normalizedOutcome = normalizeCallbackStatus(request.getOutcomeStatus());
        validateReferenceData(transaction.getCategoryId(), normalizedProviderId, normalizedOutcome);

        transaction.setStatusId(normalizedOutcome);
        transaction.setUpdatedAt(request.getCallbackTimestamp());
        if (STATUS_COMPLETED.equals(normalizedOutcome)) {
            transaction.setCompletedAt(request.getCallbackTimestamp());
        }

        PaymentTransaction saved = transactionRepository.save(transaction);
        PaymentProviderMetadata metadata = metadataRepository.findByTransactionId(saved.getId())
                .orElseGet(() -> newMetadata(saved.getId()));
        metadata.setProviderTransactionId(request.getProviderTransactionId());
        metadata.setExternalReferenceNumber(resolveExternalReference(request, metadata));
        metadata.setRawPayloadDump(request.getRawPayload());
        metadata.setErrorCode(STATUS_FAILED.equals(normalizedOutcome) ? normalizedOutcome : null);
        metadata.setErrorMessage(STATUS_FAILED.equals(normalizedOutcome) ? request.getFailureReason() : null);
        metadata.setCallbackReceivedAt(request.getCallbackTimestamp());
        if (metadata.getCreatedAt() == null) {
            metadata.setCreatedAt(LocalDateTime.now());
        }
        metadataRepository.save(metadata);

        publishTerminalEvents(saved, metadata);
        return toResponseDto(saved, metadata.getExternalReferenceNumber());
    }

    private PaymentResponseDto executeImmediateTransactionFlow(PaymentExecutionRequestDto request) {
        String normalizedCategoryId = request.getCategoryId().toUpperCase();
        String normalizedProviderId = request.getProviderId().toUpperCase();
        String normalizedStatusId = STATUS_COMPLETED;

        validateReferenceData(normalizedCategoryId, normalizedProviderId, normalizedStatusId);

        String senderId = resolveOrCreateParty(request.getSenderPartyReference());
        String receiverId = resolveOrCreateParty(request.getReceiverPartyReference());
        String transactionId = "tx_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime standardNow = LocalDateTime.now();

        PaymentTransaction savedTx = transactionRepository.save(buildTransaction(
                request,
                transactionId,
                normalizedCategoryId,
                normalizedProviderId,
                normalizedStatusId,
                senderId,
                receiverId,
                standardNow,
                standardNow
        ));

        String fakeExtRef = normalizedProviderId + "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PaymentProviderMetadata metadata = newMetadata(transactionId);
        metadata.setExternalReferenceNumber(fakeExtRef);
        metadata.setProviderTransactionId(normalizedProviderId + "TX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        metadata.setRawPayloadDump("{\"execution\":\"SUCCESS\",\"simulated_provider\":\"" + normalizedProviderId + "\"}");
        metadata.setCreatedAt(standardNow);
        metadataRepository.save(metadata);

        publishPaymentEvent(savedTx, fakeExtRef);
        return toResponseDto(savedTx, fakeExtRef);
    }

    private PaymentResponseDto executeGatewayInitiationFlow(PaymentExecutionRequestDto request) {
        String normalizedCategoryId = request.getCategoryId().toUpperCase();
        String normalizedProviderId = request.getProviderId().toUpperCase();
        String normalizedStatusId = STATUS_PROCESSING;

        validateReferenceData(normalizedCategoryId, normalizedProviderId, normalizedStatusId);

        String senderId = resolveOrCreateParty(request.getSenderPartyReference());
        String receiverId = resolveOrCreateParty(request.getReceiverPartyReference());
        String transactionId = "tx_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        PaymentTransaction savedTx = transactionRepository.save(buildTransaction(
                request,
                transactionId,
                normalizedCategoryId,
                normalizedProviderId,
                normalizedStatusId,
                senderId,
                receiverId,
                now,
                null
        ));

        PaymentGatewayInitiationResult gatewayResult = paymentGatewayClient.initiatePayment(new PaymentGatewayRequest(
                savedTx.getId(),
                savedTx.getProviderId(),
                savedTx.getAccountReference(),
                request.getReceiverPartyReference(),
                savedTx.getAmount(),
                savedTx.getCurrency()
        ));

        PaymentProviderMetadata metadata = newMetadata(savedTx.getId());
        metadata.setExternalReferenceNumber(gatewayResult.externalReferenceNumber());
        metadata.setProviderTransactionId(gatewayResult.providerTransactionId());
        metadata.setRawPayloadDump(gatewayResult.rawPayload());
        metadata.setCreatedAt(now);
        metadataRepository.save(metadata);

        return toResponseDto(savedTx, gatewayResult.externalReferenceNumber());
    }

    private PaymentResponseDto createRepaymentFromCallback(String providerId, PaymentProviderCallbackRequestDto request) {
        if (request.getAccountReference() == null || request.getAccountReference().isBlank()) {
            throw new IllegalArgumentException("Unknown transaction callback must include accountReference.");
        }

        String categoryId = request.getCategoryId() == null || request.getCategoryId().isBlank()
                ? CATEGORY_REPAYMENT
                : request.getCategoryId().toUpperCase();
        String normalizedOutcome = normalizeCallbackStatus(request.getOutcomeStatus());
        validateReferenceData(categoryId, providerId, normalizedOutcome);

        PaymentExecutionRequestDto createRequest = new PaymentExecutionRequestDto();
        createRequest.setIdempotencyKey(resolveCallbackIdempotencyKey(providerId, request));
        createRequest.setCategoryId(categoryId);
        createRequest.setProviderId(providerId);
        createRequest.setAccountReference(request.getAccountReference());
        createRequest.setLoanAccountId(null);
        createRequest.setProfileId(request.getProfileId());
        createRequest.setSenderPartyReference(request.getProfileId() == null || request.getProfileId().isBlank()
                ? EXTERNAL_PAYER_REFERENCE
                : request.getProfileId());
        createRequest.setReceiverPartyReference(LENDER_TREASURY_REFERENCE);
        createRequest.setAmount(request.getAmount());
        createRequest.setCurrency(request.getCurrency());

        String senderId = resolveOrCreateParty(createRequest.getSenderPartyReference());
        String receiverId = resolveOrCreateParty(createRequest.getReceiverPartyReference());
        String transactionId = "tx_" + UUID.randomUUID().toString().replace("-", "");

        PaymentTransaction saved = transactionRepository.save(buildTransaction(
                createRequest,
                transactionId,
                categoryId,
                providerId,
                normalizedOutcome,
                senderId,
                receiverId,
                request.getCallbackTimestamp(),
                STATUS_COMPLETED.equals(normalizedOutcome) ? request.getCallbackTimestamp() : null
        ));

        PaymentProviderMetadata metadata = newMetadata(saved.getId());
        metadata.setProviderTransactionId(request.getProviderTransactionId());
        metadata.setExternalReferenceNumber(request.getExternalReferenceNumber());
        metadata.setRawPayloadDump(request.getRawPayload());
        metadata.setErrorCode(STATUS_FAILED.equals(normalizedOutcome) ? normalizedOutcome : null);
        metadata.setErrorMessage(STATUS_FAILED.equals(normalizedOutcome) ? request.getFailureReason() : null);
        metadata.setCallbackReceivedAt(request.getCallbackTimestamp());
        metadata.setCreatedAt(LocalDateTime.now());
        metadataRepository.save(metadata);

        publishTerminalEvents(saved, metadata);
        return toResponseDto(saved, metadata.getExternalReferenceNumber());
    }

    private PaymentTransaction buildTransaction(
            PaymentExecutionRequestDto request,
            String transactionId,
            String categoryId,
            String providerId,
            String statusId,
            String senderId,
            String receiverId,
            LocalDateTime initiatedAt,
            LocalDateTime completedAt) {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(transactionId);
        transaction.setIdempotencyKey(request.getIdempotencyKey());
        transaction.setCategoryId(categoryId);
        transaction.setProviderId(providerId);
        transaction.setStatusId(statusId);
        transaction.setAccountReference(request.getAccountReference());
        transaction.setLoanAccountId(request.getLoanAccountId());
        transaction.setProfileId(request.getProfileId());
        transaction.setSenderPartyId(senderId);
        transaction.setReceiverPartyId(receiverId);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency().toUpperCase());
        transaction.setInitiatedAt(initiatedAt);
        transaction.setCompletedAt(completedAt);
        transaction.setUpdatedAt(initiatedAt);
        return transaction;
    }

    private Optional<PaymentTransaction> resolveExistingTransaction(PaymentProviderCallbackRequestDto request) {
        if (request.getInternalTransactionId() != null && !request.getInternalTransactionId().isBlank()) {
            return transactionRepository.findById(request.getInternalTransactionId());
        }
        if (request.getProviderTransactionId() != null && !request.getProviderTransactionId().isBlank()) {
            Optional<PaymentProviderMetadata> metadata = metadataRepository.findByProviderTransactionId(request.getProviderTransactionId());
            if (metadata.isPresent()) {
                return transactionRepository.findById(metadata.get().getTransactionId());
            }
        }
        if (request.getExternalReferenceNumber() != null && !request.getExternalReferenceNumber().isBlank()) {
            Optional<PaymentProviderMetadata> metadata = metadataRepository.findByExternalReferenceNumber(request.getExternalReferenceNumber());
            if (metadata.isPresent()) {
                return transactionRepository.findById(metadata.get().getTransactionId());
            }
        }
        return Optional.empty();
    }

    private String resolveOrCreateParty(String reference) {
        return partyRepository.findByPartyReference(reference)
                .map(PaymentParty::getId)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    PaymentParty dynamicParty = new PaymentParty();
                    dynamicParty.setId("part_" + UUID.randomUUID().toString().replace("-", ""));
                    dynamicParty.setPartyReference(reference);
                    dynamicParty.setDisplayName("Automated Party Mapping: " + reference);
                    dynamicParty.setPartyType(DYNAMIC_PARTY_TYPE);
                    dynamicParty.setSourceModule(DYNAMIC_PARTY_SOURCE_MODULE);
                    dynamicParty.setCreatedAt(now);
                    dynamicParty.setUpdatedAt(now);
                    return partyRepository.save(dynamicParty).getId();
                });
    }

    private void validateReferenceData(String categoryId, String providerId, String statusId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Payment category not found: " + categoryId);
        }
        if (!providerRepository.existsById(providerId)) {
            throw new IllegalArgumentException("Payment provider not found: " + providerId);
        }
        if (!transactionStatusRepository.existsById(statusId)) {
            throw new IllegalArgumentException("Payment transaction status not found: " + statusId);
        }
    }

    private boolean isTerminalStatus(String statusId) {
        return STATUS_COMPLETED.equalsIgnoreCase(statusId)
                || STATUS_FAILED.equalsIgnoreCase(statusId)
                || STATUS_REVERSED.equalsIgnoreCase(statusId);
    }

    private String normalizeCallbackStatus(String outcomeStatus) {
        String normalized = outcomeStatus.toUpperCase();
        if (!normalized.equals(STATUS_COMPLETED) && !normalized.equals(STATUS_FAILED)) {
            throw new IllegalArgumentException("Unsupported callback outcome status: " + outcomeStatus);
        }
        return normalized;
    }

    private void publishTerminalEvents(PaymentTransaction transaction, PaymentProviderMetadata metadata) {
        if (!isTerminalStatus(transaction.getStatusId())) {
            return;
        }

        publishPaymentEvent(transaction, metadata.getExternalReferenceNumber());

        if (CATEGORY_DISBURSEMENT.equalsIgnoreCase(transaction.getCategoryId())
                && STATUS_COMPLETED.equalsIgnoreCase(transaction.getStatusId())) {
            eventPublisher.publishEvent(new ProviderPayoutCompletedEvent(
                    transaction.getLoanAccountId(),
                    transaction.getId(),
                    transaction.getProfileId(),
                    transaction.getAccountReference(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getProviderId(),
                    metadata.getExternalReferenceNumber(),
                    ZonedDateTime.now()
            ));
        }
    }

    private void publishPaymentEvent(PaymentTransaction transaction, String externalReferenceNumber) {
        eventPublisher.publishEvent(new PaymentEvent(
                transaction.getId(),
                transaction.getProfileId(),
                transaction.getAccountReference(),
                transaction.getCategoryId(),
                transaction.getProviderId(),
                transaction.getStatusId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                externalReferenceNumber,
                transaction.getCompletedAt() != null ? transaction.getCompletedAt() : transaction.getUpdatedAt()
        ));
    }

    private PaymentProviderMetadata newMetadata(String transactionId) {
        PaymentProviderMetadata metadata = new PaymentProviderMetadata();
        metadata.setId("meta_" + UUID.randomUUID().toString().replace("-", ""));
        metadata.setTransactionId(transactionId);
        return metadata;
    }

    private String resolveExternalReference(PaymentProviderCallbackRequestDto request, PaymentProviderMetadata existingMetadata) {
        if (request.getExternalReferenceNumber() != null && !request.getExternalReferenceNumber().isBlank()) {
            return request.getExternalReferenceNumber();
        }
        return existingMetadata.getExternalReferenceNumber();
    }

    private String resolveCallbackIdempotencyKey(String providerId, PaymentProviderCallbackRequestDto request) {
        if (request.getProviderTransactionId() != null && !request.getProviderTransactionId().isBlank()) {
            return "callback-" + providerId + "-" + request.getProviderTransactionId();
        }
        if (request.getExternalReferenceNumber() != null && !request.getExternalReferenceNumber().isBlank()) {
            return "callback-" + providerId + "-" + request.getExternalReferenceNumber();
        }
        return "callback-" + providerId + "-" + request.getAccountReference() + "-" + request.getCallbackTimestamp();
    }

    private PaymentResponseDto toResponseDto(PaymentTransaction tx, String externalReferenceNumber) {
        return new PaymentResponseDto(
                tx.getId(),
                tx.getCategoryId(),
                tx.getProviderId(),
                tx.getStatusId(),
                tx.getAccountReference(),
                tx.getAmount(),
                tx.getCurrency(),
                externalReferenceNumber,
                tx.getCompletedAt()
        );
    }

    private PaymentResponseDto mapToResponseDto(PaymentTransaction tx) {
        String externalReference = metadataRepository.findByTransactionId(tx.getId())
                .map(PaymentProviderMetadata::getExternalReferenceNumber)
                .orElse(CACHED_IDEMPOTENCY_LIMIT_REFERENCE);
        return toResponseDto(tx, externalReference);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getFilteredPayments(
            LocalDateTime fromDate, LocalDateTime toDate, String profileId,
            String accountReference, String providerId, String currency, Pageable pageable) {

        Specification<PaymentTransaction> spec = PaymentSpecification.createSpecification(
                fromDate, toDate, profileId, accountReference, providerId, currency
        );

        return transactionRepository.findAll(spec, pageable)
                .map(this::mapToResponseDto);
    }
}
