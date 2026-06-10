package com.digital.lending.payment.service;

import com.digital.lending.payment.dto.PaymentExecutionRequestDto;
import com.digital.lending.payment.dto.PaymentResponseDto;
import com.digital.lending.payment.event.PaymentEvent;
import com.digital.lending.payment.model.PaymentParty;
import com.digital.lending.payment.model.PaymentProviderMetadata;
import com.digital.lending.payment.model.PaymentTransaction;
import com.digital.lending.payment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentPartyRepository partyRepository;
    private final PaymentProviderRepository providerRepository;
    private final PaymentCategoryRepository categoryRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final PaymentProviderMetadataRepository metadataRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponseDto registerAndProcessPayment(PaymentExecutionRequestDto request) {
        // Idempotency Validation Strategy
        return transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(this::mapToResponseDto)
                .orElseGet(() -> executeNewTransactionFlow(request));
    }

    private PaymentResponseDto executeNewTransactionFlow(PaymentExecutionRequestDto request) {
        String normalizedCategoryId = request.getCategoryId().toUpperCase();
        String normalizedProviderId = request.getProviderId().toUpperCase();
        String normalizedStatusId = "COMPLETED";

        validateReferenceData(normalizedCategoryId, normalizedProviderId, normalizedStatusId);

        String senderId = resolveOrCreateParty(request.getSenderPartyReference());
        String receiverId = resolveOrCreateParty(request.getReceiverPartyReference());

        String transactionId = "tx_" + UUID.randomUUID().toString().replace("-", "");

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(transactionId);
        transaction.setIdempotencyKey(request.getIdempotencyKey());
        transaction.setCategoryId(normalizedCategoryId);
        transaction.setProviderId(normalizedProviderId);
        transaction.setStatusId(normalizedStatusId); // In production, this transitions based on sync/async processor routes
        transaction.setAccountReference(request.getAccountReference());
        transaction.setSenderPartyId(senderId);
        transaction.setReceiverPartyId(receiverId);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency().toUpperCase());

        LocalDateTime standardNow = LocalDateTime.now();
        transaction.setInitiatedAt(standardNow);
        transaction.setCompletedAt(standardNow);
        transaction.setUpdatedAt(standardNow);

        PaymentTransaction savedTx = transactionRepository.save(transaction);

        // Generate synthetic external reference receipt to represent upstream core approval clearing networks
        String fakeExtRef = request.getProviderId().toUpperCase() + "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentProviderMetadata metadata = new PaymentProviderMetadata();
        metadata.setId("meta_" + UUID.randomUUID().toString().replace("-", ""));
        metadata.setTransactionId(transactionId);
        metadata.setExternalReferenceNumber(fakeExtRef);
        metadata.setRawPayloadDump("{\"execution\":\"SUCCESS\",\"simulated_provider\":\"" + request.getProviderId() + "\"}");
        metadata.setCreatedAt(standardNow);

        metadataRepository.save(metadata);

        // Publish outbound payment notification to decouple modules from core lending domain logic
        eventPublisher.publishEvent(new PaymentEvent(
                this,
                savedTx.getId(),
                savedTx.getAccountReference(),
                savedTx.getCategoryId(),
                savedTx.getProviderId(),
                savedTx.getStatusId(),
                savedTx.getAmount(),
                savedTx.getCurrency(),
                fakeExtRef,
                standardNow
        ));

        return new PaymentResponseDto(
                savedTx.getId(),
                savedTx.getCategoryId(),
                savedTx.getProviderId(),
                savedTx.getStatusId(),
                savedTx.getAccountReference(),
                savedTx.getAmount(),
                savedTx.getCurrency(),
                fakeExtRef,
                savedTx.getCompletedAt()
        );
    }

    private String resolveOrCreateParty(String reference) {
        return partyRepository.findByPartyReference(reference)
                .map(PaymentParty::getId)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();

                    PaymentParty dynamicParty = new PaymentParty();
                    dynamicParty.setId("part_" + UUID.randomUUID().toString().replace("-", ""));
                    dynamicParty.setPartyReference(reference);
                    dynamicParty.setDisplayName("Automated Profile Mapping: " + reference);
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

    private PaymentResponseDto mapToResponseDto(PaymentTransaction tx) {
        return new PaymentResponseDto(
                tx.getId(), tx.getCategoryId(), tx.getProviderId(), tx.getStatusId(),
                tx.getAccountReference(), tx.getAmount(), tx.getCurrency(), "CACHED_IDEMPOTENCY_LIMIT", tx.getCompletedAt()
        );
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
