package com.digital.lending.payment;

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
import com.digital.lending.payment.repository.*;
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
    private PaymentGatewayClient paymentGatewayClient;

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
        request.setProfileId("PROF-10029");
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
            assertEquals("PROF-10029", eventCaptor.getValue().profileId());
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
    @DisplayName("initiateGatewayPayment")
    class InitiateGatewayPaymentTests {

        @Test
        @DisplayName("Should create processing disbursement and persist gateway metadata")
        void shouldInitiateGatewayPaymentSuccessfully() {
            request.setCategoryId("disbursement");
            request.setProviderId("internal");

            when(transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());
            when(categoryRepository.existsById("DISBURSEMENT")).thenReturn(true);
            when(providerRepository.existsById("INTERNAL")).thenReturn(true);
            when(transactionStatusRepository.existsById("PROCESSING")).thenReturn(true);
            when(partyRepository.findByPartyReference("PART-CUST-10029")).thenReturn(Optional.empty());
            when(partyRepository.findByPartyReference("PART-CO-DISBURSE-01")).thenReturn(Optional.empty());
            when(partyRepository.save(any(PaymentParty.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(metadataRepository.save(any(PaymentProviderMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentGatewayClient.initiatePayment(any(PaymentGatewayRequest.class)))
                    .thenReturn(new PaymentGatewayInitiationResult("INTREF001", "PROVIDER-TX-001", "{\"provider\":\"INTERNAL\"}"));

            PaymentResponseDto response = service.initiateGatewayPayment(request);

            assertEquals("DISBURSEMENT", response.category());
            assertEquals("INTERNAL", response.provider());
            assertEquals("PROCESSING", response.status());
            assertEquals("INTREF001", response.externalReferenceNumber());

            ArgumentCaptor<PaymentGatewayRequest> gatewayCaptor = ArgumentCaptor.forClass(PaymentGatewayRequest.class);
            verify(paymentGatewayClient).initiatePayment(gatewayCaptor.capture());
            assertEquals("INTERNAL", gatewayCaptor.getValue().providerId());
            assertEquals("LN-2026-99102", gatewayCaptor.getValue().accountReference());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("processProviderCallback")
    class ProcessProviderCallbackTests {

        private PaymentProviderCallbackRequestDto callbackRequest;

        @BeforeEach
        void setUp() {
            callbackRequest = new PaymentProviderCallbackRequestDto();
            callbackRequest.setInternalTransactionId("tx_disb_1");
            callbackRequest.setProviderTransactionId("PROVIDER-TX-001");
            callbackRequest.setExternalReferenceNumber("EXT-001");
            callbackRequest.setOutcomeStatus("COMPLETED");
            callbackRequest.setAccountReference("LN-2026-99102");
            callbackRequest.setProfileId("PROF-10029");
            callbackRequest.setCategoryId("REPAYMENT");
            callbackRequest.setAmount(new BigDecimal("7500.00"));
            callbackRequest.setCurrency("KES");
            callbackRequest.setRawPayload("{\"status\":\"COMPLETED\"}");
            callbackRequest.setCallbackTimestamp(LocalDateTime.of(2026, 6, 10, 10, 30));
        }

        @Test
        @DisplayName("Should finalize an existing disbursement and publish terminal events")
        void shouldFinalizeExistingDisbursementCallback() {
            PaymentTransaction existing = new PaymentTransaction();
            existing.setId("tx_disb_1");
            existing.setLoanAccountId("acc_1");
            existing.setProfileId("PROF-10029");
            existing.setProviderId("INTERNAL");
            existing.setCategoryId("DISBURSEMENT");
            existing.setStatusId("PROCESSING");
            existing.setAccountReference("LN-2026-99102");
            existing.setAmount(new BigDecimal("7500.00"));
            existing.setCurrency("KES");

            PaymentProviderMetadata existingMetadata = new PaymentProviderMetadata();
            existingMetadata.setId("meta_1");
            existingMetadata.setTransactionId("tx_disb_1");
            existingMetadata.setExternalReferenceNumber("EXT-001");

            when(transactionRepository.findById("tx_disb_1")).thenReturn(Optional.of(existing));
            when(categoryRepository.existsById("DISBURSEMENT")).thenReturn(true);
            when(providerRepository.existsById("INTERNAL")).thenReturn(true);
            when(transactionStatusRepository.existsById("COMPLETED")).thenReturn(true);
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(metadataRepository.findByTransactionId("tx_disb_1")).thenReturn(Optional.of(existingMetadata));
            when(metadataRepository.save(any(PaymentProviderMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto response = service.processProviderCallback("internal", callbackRequest);

            assertEquals("COMPLETED", response.status());
            assertEquals("EXT-001", response.externalReferenceNumber());
            assertEquals(callbackRequest.getCallbackTimestamp(), existing.getCompletedAt());

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
            assertTrue(eventCaptor.getAllValues().stream().anyMatch(PaymentEvent.class::isInstance));
            assertTrue(eventCaptor.getAllValues().stream().anyMatch(ProviderPayoutCompletedEvent.class::isInstance));
        }

        @Test
        @DisplayName("Should create repayment transaction from callback when no existing transaction matches")
        void shouldCreateRepaymentFromUnknownCallback() {
            callbackRequest.setInternalTransactionId(null);
            callbackRequest.setProviderTransactionId("MPESA-TX-777");
            callbackRequest.setExternalReferenceNumber("MPESA-EXT-777");

            when(metadataRepository.findByProviderTransactionId("MPESA-TX-777")).thenReturn(Optional.empty());
            when(metadataRepository.findByExternalReferenceNumber("MPESA-EXT-777")).thenReturn(Optional.empty());
            when(categoryRepository.existsById("REPAYMENT")).thenReturn(true);
            when(providerRepository.existsById("MPESA")).thenReturn(true);
            when(transactionStatusRepository.existsById("COMPLETED")).thenReturn(true);
            when(partyRepository.findByPartyReference("PROF-10029")).thenReturn(Optional.empty());
            when(partyRepository.findByPartyReference("LENDER_TREASURY")).thenReturn(Optional.empty());
            when(partyRepository.save(any(PaymentParty.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(metadataRepository.save(any(PaymentProviderMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto response = service.processProviderCallback("mpesa", callbackRequest);

            assertEquals("REPAYMENT", response.category());
            assertEquals("COMPLETED", response.status());
            verify(eventPublisher, times(1)).publishEvent(any(PaymentEvent.class));
            verify(eventPublisher, never()).publishEvent(any(ProviderPayoutCompletedEvent.class));
        }

        @Test
        @DisplayName("Should return cached response when callback is replayed for a terminal transaction")
        void shouldReturnCachedResponseForTerminalTransaction() {
            PaymentTransaction existing = new PaymentTransaction();
            existing.setId("tx_done");
            existing.setProviderId("MPESA");
            existing.setCategoryId("REPAYMENT");
            existing.setStatusId("COMPLETED");
            existing.setAccountReference("LN-2026-99102");
            existing.setAmount(new BigDecimal("7500.00"));
            existing.setCurrency("KES");
            existing.setCompletedAt(LocalDateTime.of(2026, 6, 10, 10, 31));

            PaymentProviderMetadata metadata = new PaymentProviderMetadata();
            metadata.setTransactionId("tx_done");
            metadata.setExternalReferenceNumber("EXT-CACHED");

            callbackRequest.setInternalTransactionId("tx_done");
            when(transactionRepository.findById("tx_done")).thenReturn(Optional.of(existing));
            when(metadataRepository.findByTransactionId("tx_done")).thenReturn(Optional.of(metadata));

            PaymentResponseDto response = service.processProviderCallback("mpesa", callbackRequest);

            assertEquals("tx_done", response.id());
            assertEquals("EXT-CACHED", response.externalReferenceNumber());
            verify(transactionRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
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
