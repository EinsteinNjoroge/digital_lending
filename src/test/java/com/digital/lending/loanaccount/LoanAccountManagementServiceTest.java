package com.digital.lending.loanaccount;

import com.digital.lending.events.LoanAccountSettledEvent;
import com.digital.lending.events.LoanApplicationApprovedEvent;
import com.digital.lending.events.LoanApplicationCreatedEvent;
import com.digital.lending.events.LoanApplicationRejectedEvent;
import com.digital.lending.events.LoanDisbursalRequestedEvent;
import com.digital.lending.events.PaymentEvent;
import com.digital.lending.loanaccount.dto.LoanAccountOpeningRequestDto;
import com.digital.lending.loanaccount.dto.LoanAccountResponseDto;
import com.digital.lending.loanaccount.dto.StatusModificationRequestDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.exception.BusinessRuleViolationException;
import com.digital.lending.loanaccount.exception.ResourceNotFoundException;
import com.digital.lending.loanaccount.model.LoanAccount;
import com.digital.lending.loanaccount.model.LoanAccountAuditLog;
import com.digital.lending.loanaccount.repository.LoanAccountAuditLogRepository;
import com.digital.lending.loanaccount.repository.LoanAccountRepository;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanAccountManagementServiceTest {

    @Mock
    private LoanAccountRepository accountRepository;

    @Mock
    private LoanAccountAuditLogRepository auditLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LoanAccountManagementService service;

    private LoanAccountOpeningRequestDto sampleRequest;
    private static final String ACTOR = "system_test_runner";

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        lenient().when(auditLogRepository.save(any(LoanAccountAuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        sampleRequest = new LoanAccountOpeningRequestDto();
        sampleRequest.setProfileId("PROF-10029");
        sampleRequest.setLoanProductId(UUID.randomUUID().toString());
        sampleRequest.setIdempotencyKey("idem_key_9921");
        sampleRequest.setInitialPrincipal(new BigDecimal("15000.00"));
        sampleRequest.setParentLoanAccountId(null);
        sampleRequest.setPartnerId("SAF_KE_01");
        sampleRequest.setCurrency("KES");
        sampleRequest.setScoringFeatures(Map.of("wallet_throughput_30d", "250000.00"));
        sampleRequest.setDisbursementProviderId("INTERNAL");
        sampleRequest.setDisbursementDestinationReference("WALLET-PROF-10029");
    }

    @Nested
    @DisplayName("Method: provisionNewAccount")
    class ProvisionNewAccountTests {

        @Test
        @DisplayName("Should return existing account immediately if idempotency key is already cached")
        void shouldReturnExistingAccountOnIdempotencyHit() {
            LoanAccount existingAccount = new LoanAccount();
            existingAccount.setId("acc_existing");
            existingAccount.setProfileId("PROF-10029");
            existingAccount.setIdempotencyKey(sampleRequest.getIdempotencyKey());
            existingAccount.setIssuanceStatus(IssuanceStatus.PENDING_SCORE_VALIDATION);

            when(accountRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                    .thenReturn(Optional.of(existingAccount));

            LoanAccountResponseDto result = service.provisionNewAccount(sampleRequest, ACTOR);

            assertEquals("acc_existing", result.getId());
            verify(accountRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleViolationException if profile already holds blocked active loan exposures")
        void shouldThrowExceptionWhenActiveExposureDiscovered() {
            when(accountRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey())).thenReturn(Optional.empty());
            when(accountRepository.existsByProfileIdAndLoanProductIdAndPerformanceStatusIn(eq(sampleRequest.getProfileId()), eq(sampleRequest.getLoanProductId()), any()))
                    .thenReturn(true);

            assertThrows(BusinessRuleViolationException.class, () -> service.provisionNewAccount(sampleRequest, ACTOR));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should persist account in PENDING_SCORE_VALIDATION and publish LoanApplicationCreatedEvent")
        void shouldProvisionPendingAccountCleanly() {
            when(accountRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey())).thenReturn(Optional.empty());
            when(accountRepository.existsByProfileIdAndLoanProductIdAndPerformanceStatusIn(any(), any(), any())).thenReturn(false);
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanAccountResponseDto response = service.provisionNewAccount(sampleRequest, ACTOR);

            assertEquals(IssuanceStatus.PENDING_SCORE_VALIDATION, response.getIssuanceStatus());
            assertEquals(new BigDecimal("15000.00"), response.getOutstandingPrincipal());

            ArgumentCaptor<LoanApplicationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(LoanApplicationCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(response.getId(), eventCaptor.getValue().loanAccountId());
        }
    }

    @Nested
    @DisplayName("Method: application decision processing")
    class ApplicationDecisionTests {

        private LoanAccount pendingAccount;

        @BeforeEach
        void setUp() {
            pendingAccount = new LoanAccount();
            pendingAccount.setId("acc_test_id");
            pendingAccount.setProfileId("PROF-10029");
            pendingAccount.setLoanProductId(UUID.randomUUID().toString());
            pendingAccount.setInitialPrincipal(new BigDecimal("10000.00"));
            pendingAccount.setOutstandingPrincipal(new BigDecimal("10000.00"));
            pendingAccount.setIssuanceStatus(IssuanceStatus.PENDING_SCORE_VALIDATION);
        }

        @Test
        void shouldAbortApprovalIfAccountDoesNotExist() {
            when(accountRepository.findById("acc_missing")).thenReturn(Optional.empty());

            LoanApplicationApprovedEvent event = approvalEvent("acc_missing", new BigDecimal("20000.00"));
            service.processApprovedApplication(event);

            verify(accountRepository, never()).save(any());
        }

        @Test
        void shouldSkipApprovalIfAccountIsNotPending() {
            pendingAccount.setIssuanceStatus(IssuanceStatus.ACTIVE);
            when(accountRepository.findById(pendingAccount.getId())).thenReturn(Optional.of(pendingAccount));

            service.processApprovedApplication(approvalEvent(pendingAccount.getId(), new BigDecimal("20000.00")));

            verify(accountRepository, never()).save(any());
        }

        @Test
        void shouldTransitionToDeniedOnRejectedDecisionEvent() {
            when(accountRepository.findById(pendingAccount.getId())).thenReturn(Optional.of(pendingAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanApplicationRejectedEvent rejectionEvent = new LoanApplicationRejectedEvent(
                    pendingAccount.getId(), "dec_rejected", pendingAccount.getProfileId(), pendingAccount.getLoanProductId(), pendingAccount.getInitialPrincipal(),
                    "DECLINED", "Score below threshold", 450.0, "SAF_KE_01", "KES", ACTOR, ZonedDateTime.now()
            );

            service.processRejectedApplication(rejectionEvent);

            assertEquals(IssuanceStatus.DENIED, pendingAccount.getIssuanceStatus());
        }

        @Test
        void shouldDenyIfAllocatedCreditLimitIsInsufficient() {
            pendingAccount.setInitialPrincipal(new BigDecimal("50000.00"));
            pendingAccount.setOutstandingPrincipal(new BigDecimal("50000.00"));
            when(accountRepository.findById(pendingAccount.getId())).thenReturn(Optional.of(pendingAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processApprovedApplication(approvalEvent(pendingAccount.getId(), new BigDecimal("5000.00")));

            assertEquals(IssuanceStatus.DENIED, pendingAccount.getIssuanceStatus());
            verify(eventPublisher, never()).publishEvent(any(LoanDisbursalRequestedEvent.class));
        }

        @Test
        void shouldApproveAndRequestDisbursal() {
            when(accountRepository.findById(pendingAccount.getId())).thenReturn(Optional.of(pendingAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processApprovedApplication(approvalEvent(pendingAccount.getId(), new BigDecimal("50000.00")));

            assertEquals(IssuanceStatus.APPROVED, pendingAccount.getIssuanceStatus());
            assertNotNull(pendingAccount.getAccountNumber());
            verify(eventPublisher).publishEvent(any(LoanDisbursalRequestedEvent.class));
        }

        private LoanApplicationApprovedEvent approvalEvent(String accountId, BigDecimal approvedLimit) {
            return new LoanApplicationApprovedEvent(
                    accountId, "dec_success_01", pendingAccount.getProfileId(), pendingAccount.getLoanProductId(), pendingAccount.getInitialPrincipal(),
                    approvedLimit, 780.0, "SAF_KE_01", "KES", "INTERNAL", "WALLET-PROF-10029", ACTOR, ZonedDateTime.now()
            );
        }
    }

    @Nested
    @DisplayName("Method: payment event processing")
    class PaymentEventProcessingTests {

        private LoanAccount approvedAccount;
        private LoanAccount activeAccount;

        @BeforeEach
        void setUp() {
            approvedAccount = new LoanAccount();
            approvedAccount.setId("acc_approved");
            approvedAccount.setAccountNumber("LN-2026-12345");
            approvedAccount.setProfileId("PROF-10029");
            approvedAccount.setLoanProductId(UUID.randomUUID().toString());
            approvedAccount.setInitialPrincipal(new BigDecimal("10000.00"));
            approvedAccount.setOutstandingPrincipal(new BigDecimal("10000.00"));
            approvedAccount.setIssuanceStatus(IssuanceStatus.APPROVED);
            approvedAccount.setPerformanceStatus(PerformanceStatus.ACTIVE);

            activeAccount = new LoanAccount();
            activeAccount.setId("acc_active");
            activeAccount.setAccountNumber("LN-2026-54321");
            activeAccount.setProfileId("PROF-10029");
            activeAccount.setLoanProductId(UUID.randomUUID().toString());
            activeAccount.setInitialPrincipal(new BigDecimal("10000.00"));
            activeAccount.setOutstandingPrincipal(new BigDecimal("10000.00"));
            activeAccount.setIssuanceStatus(IssuanceStatus.ACTIVE);
            activeAccount.setPerformanceStatus(PerformanceStatus.ACTIVE);
        }

        @Test
        void shouldActivateApprovedLoanOnCompletedDisbursement() {
            when(accountRepository.findByAccountNumber("LN-2026-12345")).thenReturn(Optional.of(approvedAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processPaymentEvent(new PaymentEvent(
                    "tx_1", "PROF-10029", "LN-2026-12345", "DISBURSEMENT", "INTERNAL", "COMPLETED",
                    new BigDecimal("10000.00"), "KES", "INTREF001", LocalDateTime.now()
            ));

            assertEquals(IssuanceStatus.ACTIVE, approvedAccount.getIssuanceStatus());
            assertNotNull(approvedAccount.getTakenAt());
        }

        @Test
        void shouldApplyPartialRepayment() {
            when(accountRepository.findByAccountNumber("LN-2026-54321")).thenReturn(Optional.of(activeAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processPaymentEvent(new PaymentEvent(
                    "tx_2", "PROF-10029", "LN-2026-54321", "REPAYMENT", "MPESA", "COMPLETED",
                    new BigDecimal("2500.00"), "KES", "MPREF001", LocalDateTime.now()
            ));

            assertEquals(new BigDecimal("7500.00"), activeAccount.getOutstandingPrincipal());
            assertEquals(IssuanceStatus.ACTIVE, activeAccount.getIssuanceStatus());
        }

        @Test
        void shouldSettleLoanOnFullRepayment() {
            when(accountRepository.findByAccountNumber("LN-2026-54321")).thenReturn(Optional.of(activeAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.processPaymentEvent(new PaymentEvent(
                    "tx_3", "PROF-10029", "LN-2026-54321", "REPAYMENT", "MPESA", "COMPLETED",
                    new BigDecimal("10000.00"), "KES", "MPREF002", LocalDateTime.now()
            ));

            assertEquals(0, BigDecimal.ZERO.compareTo(activeAccount.getOutstandingPrincipal()));
            assertEquals(IssuanceStatus.SETTLED, activeAccount.getIssuanceStatus());
            verify(eventPublisher).publishEvent(any(LoanAccountSettledEvent.class));
        }
    }

    @Nested
    @DisplayName("Method: modifyPerformanceStatus")
    class ModifyPerformanceStatusTests {

        private LoanAccount activeAccount;
        private StatusModificationRequestDto modificationRequest;

        @BeforeEach
        void setUp() {
            activeAccount = new LoanAccount();
            activeAccount.setId("acc_active_id");
            activeAccount.setProfileId("PROF-10029");
            activeAccount.setLoanProductId(UUID.randomUUID().toString());
            activeAccount.setIssuanceStatus(IssuanceStatus.ACTIVE);
            activeAccount.setPerformanceStatus(PerformanceStatus.ACTIVE);

            modificationRequest = new StatusModificationRequestDto();
            modificationRequest.setTargetStatus(PerformanceStatus.WATCH);
        }

        @Test
        void shouldThrowExceptionWhenAccountMissing() {
            when(accountRepository.findById("acc_nonexistent")).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.modifyPerformanceStatus("acc_nonexistent", modificationRequest, ACTOR));
        }

        @Test
        void shouldThrowExceptionIfAccountIsNotYetActive() {
            activeAccount.setIssuanceStatus(IssuanceStatus.APPROVED);
            when(accountRepository.findById(activeAccount.getId())).thenReturn(Optional.of(activeAccount));

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                    () -> service.modifyPerformanceStatus(activeAccount.getId(), modificationRequest, ACTOR));
            assertEquals("Cannot change the performance status of an unissued, denied, or settled loan account record line.", exception.getMessage());
        }

        @Test
        void shouldUpdatePerformanceStatusCleanly() {
            when(accountRepository.findById(activeAccount.getId())).thenReturn(Optional.of(activeAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanAccountResponseDto result = service.modifyPerformanceStatus(activeAccount.getId(), modificationRequest, ACTOR);

            assertEquals(PerformanceStatus.WATCH, result.getPerformanceStatus());
        }
    }
}
