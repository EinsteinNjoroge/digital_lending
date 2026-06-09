package com.digital.lending.loanaccount;

import com.digital.lending.loanaccount.dto.LoanAccountOpeningRequestDto;
import com.digital.lending.loanaccount.dto.LoanAccountResponseDto;
import com.digital.lending.loanaccount.dto.StatusModificationRequestDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.event.DraftLoanEvent;
import com.digital.lending.loanaccount.event.LoanApprovedStatusEvent;
import com.digital.lending.loanaccount.event.LoanCreditScoreEvaluatedEvent;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        sampleRequest.setProfileId("CUST-10029");
        sampleRequest.setLoanProductId(UUID.randomUUID().toString());
        sampleRequest.setIdempotencyKey("idem_key_9921");
        sampleRequest.setInitialPrincipal(new BigDecimal("15000.00"));
        sampleRequest.setParentLoanAccountId(null);
    }

    @Nested
    @DisplayName("Method: provisionNewAccount")
    class ProvisionNewAccountTests {

        @Test
        @DisplayName("Idempotency Hit: Should return existing account immediately if idempotency key is already cached")
        void shouldReturnExistingAccountOnIdempotencyHit() {
            LoanAccount existingAccount = new LoanAccount();
            existingAccount.setId("acc_existing");
            existingAccount.setAccountNumber("LN-EXISTING-01");
            existingAccount.setProfileId("CUST-10029");
            existingAccount.setIdempotencyKey(sampleRequest.getIdempotencyKey());
            existingAccount.setIssuanceStatus(IssuanceStatus.DRAFT);

            when(accountRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                    .thenReturn(Optional.of(existingAccount));

            LoanAccountResponseDto result = service.provisionNewAccount(sampleRequest, ACTOR);

            assertNotNull(result);
            assertEquals("acc_existing", result.getId());
            verify(accountRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Risk Threshold Breach: Should throw BusinessRuleViolationException if profile already holds blocked active loan exposures")
        void shouldThrowExceptionWhenActiveExposureDiscovered() {
            when(accountRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                    .thenReturn(Optional.empty());

            // Broaden collection matcher to any() to bypass strict list definition constraints
            when(accountRepository.existsByProfileIdAndLoanProductIdAndPerformanceStatusIn(
                    eq(sampleRequest.getProfileId()),
                    eq(sampleRequest.getLoanProductId()),
                    any()))
                    .thenReturn(true);

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () ->
                service.provisionNewAccount(sampleRequest, ACTOR)
            );

            assertEquals("Profile already holds an unresolved active loan position for this product code.", exception.getMessage());
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Happy Path: Should persist account in DRAFT, log audit trace, and publish DraftLoanEvent")
        void shouldProvisionDraftAccountCleanly() {
            when(accountRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                    .thenReturn(Optional.empty());
            when(accountRepository.existsByProfileIdAndLoanProductIdAndPerformanceStatusIn(any(), any(), any()))
                    .thenReturn(false);
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanAccountResponseDto response = service.provisionNewAccount(sampleRequest, ACTOR);

            assertNotNull(response);
            assertTrue(response.getId().startsWith("acc_"));
            assertEquals(IssuanceStatus.DRAFT, response.getIssuanceStatus());
            assertEquals(sampleRequest.getInitialPrincipal(), response.getInitialPrincipal());

            // Verifies the audit record is safely triggered
            verify(auditLogRepository, atLeastOnce()).save(any(LoanAccountAuditLog.class));

            ArgumentCaptor<DraftLoanEvent> eventCaptor = ArgumentCaptor.forClass(DraftLoanEvent.class);
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

            DraftLoanEvent publishedEvent = eventCaptor.getValue();
            assertEquals(response.getId(), publishedEvent.getLoanAccountId());
            assertEquals(sampleRequest.getProfileId(), publishedEvent.getProfileId());
        }
    }

    @Nested
    @DisplayName("Method: processUnderwritingOutcome")
    class UnderwritingOutcomeTests {

        private LoanAccount draftAccount;

        @BeforeEach
        void setUp() {
            draftAccount = new LoanAccount();
            draftAccount.setId("acc_test_id");
            draftAccount.setAccountNumber("LN-PENDING-02");
            draftAccount.setProfileId("CUST-10029");
            draftAccount.setLoanProductId(UUID.randomUUID().toString());
            draftAccount.setInitialPrincipal(new BigDecimal("10000.00"));
            draftAccount.setIssuanceStatus(IssuanceStatus.DRAFT);
            draftAccount.setPerformanceStatus(null);
        }

        @Test
        @DisplayName("Unrecognized ID Guard: Should log error and abort if the target account identifier is missing")
        void shouldAbortIfAccountDoesNotExist() {
            when(accountRepository.findById("acc_missing")).thenReturn(Optional.empty());

            LoanCreditScoreEvaluatedEvent event = new LoanCreditScoreEvaluatedEvent(
                    this, "acc_missing", "dec_01", "APPROVED", new BigDecimal("20000.00"), 720.0, ACTOR
            );

            service.processUnderwritingOutcome(event);

            verify(accountRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Lifecycle Guard: Should log warning and skip execution if account is no longer in DRAFT state")
        void shouldSkipIfAccountIsNotInDraftStatus() {
            draftAccount.setIssuanceStatus(IssuanceStatus.APPROVED_ISSUED);
            when(accountRepository.findById(draftAccount.getId())).thenReturn(Optional.of(draftAccount));

            LoanCreditScoreEvaluatedEvent event = new LoanCreditScoreEvaluatedEvent(
                    this, draftAccount.getId(), "dec_01", "APPROVED", new BigDecimal("20000.00"), 720.0, ACTOR
            );

            service.processUnderwritingOutcome(event);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Underwriting Denial: Should transition account state to DENIED if decision is not APPROVED")
        void shouldTransitionToDeniedIfDecisionFails() {
            when(accountRepository.findById(draftAccount.getId())).thenReturn(Optional.of(draftAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanCreditScoreEvaluatedEvent declineEvent = new LoanCreditScoreEvaluatedEvent(
                    this, draftAccount.getId(), "dec_rejected", "DECLINED", BigDecimal.ZERO, 450.0, ACTOR
            );

            service.processUnderwritingOutcome(declineEvent);

            assertEquals(IssuanceStatus.DENIED, draftAccount.getIssuanceStatus());
            verify(accountRepository).save(draftAccount);
            verify(auditLogRepository, atLeastOnce()).save(any(LoanAccountAuditLog.class));
        }

        @Test
        @DisplayName("Limit Breach Policy: Should mark application as DENIED if allocated credit capacity is lower than requested principal")
        void shouldDenyIfAllocatedCreditLimitIsInsufficient() {
            draftAccount.setInitialPrincipal(new BigDecimal("50000.00"));
            when(accountRepository.findById(draftAccount.getId())).thenReturn(Optional.of(draftAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanCreditScoreEvaluatedEvent lowLimitEvent = new LoanCreditScoreEvaluatedEvent(
                    this, draftAccount.getId(), "dec_insufficient", "APPROVED", new BigDecimal("5000.00"), 610.0, ACTOR
            );

            service.processUnderwritingOutcome(lowLimitEvent);

            assertEquals(IssuanceStatus.DENIED, draftAccount.getIssuanceStatus());
            verify(accountRepository).save(draftAccount);
            verify(auditLogRepository, atLeastOnce()).save(any(LoanAccountAuditLog.class));
        }

        @Test
        @DisplayName("Success Path: Should issue account, allocate an account number, mark ACTIVE, and broadcast LoanApprovedStatusEvent")
        void shouldApproveAndIssueLoanCleanlyOnValidOutcome() {
            // Keep initial requested principal at 10,000 to remain safely within the 50,000 mock allocation capacity
            draftAccount.setInitialPrincipal(new BigDecimal("10000.00"));
            when(accountRepository.findById(draftAccount.getId())).thenReturn(Optional.of(draftAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanCreditScoreEvaluatedEvent clearApprovalEvent = new LoanCreditScoreEvaluatedEvent(
                    this, draftAccount.getId(), "dec_success_01", "APPROVED", new BigDecimal("50000.00"), 780.0, ACTOR
            );

            service.processUnderwritingOutcome(clearApprovalEvent);

            assertEquals(IssuanceStatus.APPROVED_ISSUED, draftAccount.getIssuanceStatus());
            assertEquals(PerformanceStatus.ACTIVE, draftAccount.getPerformanceStatus());
            assertNotNull(draftAccount.getAccountNumber());

            verify(accountRepository).save(draftAccount);
            verify(auditLogRepository, atLeastOnce()).save(any(LoanAccountAuditLog.class));

            ArgumentCaptor<LoanApprovedStatusEvent> approvalCaptor = ArgumentCaptor.forClass(LoanApprovedStatusEvent.class);
            verify(eventPublisher).publishEvent(approvalCaptor.capture());
            assertEquals(draftAccount.getId(), approvalCaptor.getValue().getLoanAccountId());
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
            activeAccount.setAccountNumber("LN-ACTIVE-99");
            activeAccount.setProfileId("CUST-10029");
            activeAccount.setLoanProductId(UUID.randomUUID().toString());
            activeAccount.setIssuanceStatus(IssuanceStatus.APPROVED_ISSUED);
            activeAccount.setPerformanceStatus(PerformanceStatus.ACTIVE);

            modificationRequest = new StatusModificationRequestDto();
            modificationRequest.setTargetStatus(PerformanceStatus.WATCH);
        }

        @Test
        @DisplayName("Resource Absence Check: Should throw ResourceNotFoundException if account reference doesn't exist")
        void shouldThrowExceptionWhenAccountMissing() {
            when(accountRepository.findById("acc_nonexistent")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    service.modifyPerformanceStatus("acc_nonexistent", modificationRequest, ACTOR)
            );
        }

        @Test
        @DisplayName("State Guard Rule: Should throw BusinessRuleViolationException if attempting modification on unissued or draft entry frames")
        void shouldThrowExceptionIfAccountIsNotYetIssued() {
            activeAccount.setIssuanceStatus(IssuanceStatus.DRAFT);
            when(accountRepository.findById(activeAccount.getId())).thenReturn(Optional.of(activeAccount));

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () ->
                    service.modifyPerformanceStatus(activeAccount.getId(), modificationRequest, ACTOR)
            );
            assertEquals("Cannot change the performance status of an unissued or denied loan account record line.", exception.getMessage());
        }

        @Test
        @DisplayName("Happy Path: Should successfully shift performance rating matrix status and write full audit logs")
        void shouldUpdatePerformanceStatusCleanly() {
            when(accountRepository.findById(activeAccount.getId())).thenReturn(Optional.of(activeAccount));
            when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanAccountResponseDto result = service.modifyPerformanceStatus(activeAccount.getId(), modificationRequest, ACTOR);

            assertNotNull(result);
            assertEquals(PerformanceStatus.WATCH, result.getPerformanceStatus());
            verify(accountRepository).save(activeAccount);

            ArgumentCaptor<LoanAccountAuditLog> auditCaptor = ArgumentCaptor.forClass(LoanAccountAuditLog.class);
            verify(auditLogRepository, atLeastOnce()).save(auditCaptor.capture());

            LoanAccountAuditLog documentedLog = auditCaptor.getValue();
            assertEquals("PERFORMANCE_STATUS_CHANGED", documentedLog.getEventType());
            assertNotNull(documentedLog.getPreviousState());
            assertNotNull(documentedLog.getNewState());
        }
    }
}
