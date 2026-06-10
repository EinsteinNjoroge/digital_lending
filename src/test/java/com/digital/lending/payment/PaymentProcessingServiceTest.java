package com.digital.lending.payment;

import com.digital.lending.payment.dto.PaymentExecutionRequestDto;
import com.digital.lending.payment.dto.PaymentResponseDto;
import com.digital.lending.payment.event.PaymentEvent;
import com.digital.lending.payment.model.PaymentParty;
import com.digital.lending.payment.model.PaymentProviderMetadata;
import com.digital.lending.payment.model.PaymentTransaction;
import com.digital.lending.payment.repository.PaymentCategoryRepository;
import com.digital.lending.payment.repository.PaymentPartyRepository;
import com.digital.lending.payment.repository.PaymentProviderMetadataRepository;
import com.digital.lending.payment.repository.PaymentProviderRepository;
import com.digital.lending.payment.repository.PaymentTransactionRepository;
import com.digital.lending.payment.repository.TransactionStatusRepository;
import com.digital.lending.payment.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentPartyRepository partyRepository;

    @Mock
    private PaymentProviderRepository providerRepository;

    @Mock
    private PaymentCategoryRepository categoryRepository;

    @Mock
    private TransactionStatusRepository transactionStatusRepository;

    @Mock
    private PaymentProviderMetadataRepository metadataRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentProcessingService service;

    private PaymentExecutionRequestDto request;

    @BeforeEach
    void setUp() {
        request = new PaymentExecutionRequestDto();
        request.setIdempotencyKey("idem-key-88192-332");
        request.setCategoryId("repayment");
        request.setProviderId("mpesa");
        request.setAccountReference("LN-2026-99102");
        request.setSenderPartyReference("PART-CUST-10029");
        request.setReceiverPartyReference("PART-CO-DISBURSE-01");
        request.setAmount(new BigDecimal("7500.00"));
        request.setCurrency("kes");
    }

    @Nested
    @DisplayName("registerAndProcessPayment")
    class RegisterAndProcessPaymentTests {

        @Test
        @DisplayName("Should return cached transaction response on idempotency replay")
        void shouldReturnCachedTransactionResponseOnIdempotencyReplay() {
            PaymentTransaction existing = new PaymentTransaction();
            existing.setId("tx_existing");
            existing.setCategoryId("REPAYMENT");
            existing.setProviderId("MPESA");
            existing.setStatusId("COMPLETED");
            existing.setAccountReference("LN-2026-99102");
            existing.setAmount(new BigDecimal("7500.00"));
            existing.setCurrency("KES");
            existing.setCompletedAt(LocalDateTime.of(2026, 6, 10, 10, 0));

            when(transactionRepository.findByIdempotencyKey("idem-key-88192-332"))
                    .thenReturn(Optional.of(existing));

            PaymentResponseDto response = service.registerAndProcessPayment(request);

            assertEquals("tx_existing", response.id());
            assertEquals("CACHED_IDEMPOTENCY_LIMIT", response.externalReferenceNumber());
            verify(transactionRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should create transaction, metadata, parties and publish event on fresh payment")
        void shouldCreateFreshPaymentSuccessfully() {
            when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
            when(categoryRepository.existsById("REPAYMENT")).thenReturn(true);
            when(providerRepository.existsById("MPESA")).thenReturn(true);
            when(transactionStatusRepository.existsById("COMPLETED")).thenReturn(true);
            when(partyRepository.findByPartyReference("PART-CUST-10029")).thenReturn(Optional.empty());
            when(partyRepository.findByPartyReference("PART-CO-DISBURSE-01")).thenReturn(Optional.empty());
            when(partyRepository.save(any(PaymentParty.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(metadataRepository.save(any(PaymentProviderMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto response = service.registerAndProcessPayment(request);

            assertNotNull(response.id());
            assertEquals("REPAYMENT", response.category());
            assertEquals("MPESA", response.provider());
            assertEquals("COMPLETED", response.status());
            assertEquals("KES", response.currency());

            ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(transactionRepository).save(txCaptor.capture());
            assertEquals("REPAYMENT", txCaptor.getValue().getCategoryId());
            assertEquals("MPESA", txCaptor.getValue().getProviderId());
            assertEquals("KES", txCaptor.getValue().getCurrency());

            verify(partyRepository, times(2)).save(any(PaymentParty.class));
            verify(metadataRepository).save(any(PaymentProviderMetadata.class));

            ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(response.id(), eventCaptor.getValue().transactionId());
            assertEquals("MPESA", eventCaptor.getValue().providerId());
        }

        @Test
        @DisplayName("Should reuse existing parties instead of creating new ones")
        void shouldReuseExistingParties() {
            PaymentParty sender = new PaymentParty();
            sender.setId("part_sender");
            PaymentParty receiver = new PaymentParty();
            receiver.setId("part_receiver");

            when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
            when(categoryRepository.existsById("REPAYMENT")).thenReturn(true);
            when(providerRepository.existsById("MPESA")).thenReturn(true);
            when(transactionStatusRepository.existsById("COMPLETED")).thenReturn(true);
            when(partyRepository.findByPartyReference("PART-CUST-10029")).thenReturn(Optional.of(sender));
            when(partyRepository.findByPartyReference("PART-CO-DISBURSE-01")).thenReturn(Optional.of(receiver));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(metadataRepository.save(any(PaymentProviderMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto response = service.registerAndProcessPayment(request);

            assertNotNull(response.id());
            verify(partyRepository, never()).save(any(PaymentParty.class));
        }

        @Test
        @DisplayName("Should fail when category reference data is missing")
        void shouldFailWhenCategoryMissing() {
            when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
            when(categoryRepository.existsById("REPAYMENT")).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerAndProcessPayment(request));

            assertEquals("Payment category not found: REPAYMENT", ex.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fail when provider reference data is missing")
        void shouldFailWhenProviderMissing() {
            when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
            when(categoryRepository.existsById("REPAYMENT")).thenReturn(true);
            when(providerRepository.existsById("MPESA")).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerAndProcessPayment(request));

            assertEquals("Payment provider not found: MPESA", ex.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fail when transaction status reference data is missing")
        void shouldFailWhenStatusMissing() {
            when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
            when(categoryRepository.existsById("REPAYMENT")).thenReturn(true);
            when(providerRepository.existsById("MPESA")).thenReturn(true);
            when(transactionStatusRepository.existsById("COMPLETED")).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerAndProcessPayment(request));

            assertEquals("Payment transaction status not found: COMPLETED", ex.getMessage());
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getFilteredPayments")
    class GetFilteredPaymentsTests {

        @Test
        @DisplayName("Should delegate filters and map paged transactions to response DTOs")
        void shouldMapFilteredTransactions() {
            PaymentTransaction tx = new PaymentTransaction();
            tx.setId("tx_filter_1");
            tx.setCategoryId("REPAYMENT");
            tx.setProviderId("MPESA");
            tx.setStatusId("COMPLETED");
            tx.setAccountReference("LN-2026-99102");
            tx.setAmount(new BigDecimal("7500.00"));
            tx.setCurrency("KES");
            tx.setCompletedAt(LocalDateTime.of(2026, 6, 10, 12, 0));

            when(transactionRepository.findAll(org.mockito.ArgumentMatchers.<Specification<PaymentTransaction>>any(), eq(PageRequest.of(0, 20))))
                    .thenReturn(new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1));

            Page<PaymentResponseDto> result = service.getFilteredPayments(
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59),
                    "PART-CUST-10029",
                    "LN-2026-99102",
                    "MPESA",
                    "KES",
                    PageRequest.of(0, 20)
            );

            assertEquals(1, result.getTotalElements());
            assertEquals("tx_filter_1", result.getContent().getFirst().id());
            assertEquals("REPAYMENT", result.getContent().getFirst().category());
        }
    }
}
