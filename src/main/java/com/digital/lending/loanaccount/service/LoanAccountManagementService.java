package com.digital.lending.loanaccount.service;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanAccountManagementService {

    private final LoanAccountRepository accountRepository;
    private final LoanAccountAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanAccountResponseDto provisionNewAccount(LoanAccountOpeningRequestDto request, String actor) {
        // 1. Idempotency Intercept Check
        Optional<LoanAccount> existingHolder = accountRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingHolder.isPresent()) {
            return convertToResponse(existingHolder.get());
        }

        // 2. Policy Check: Block overlapping active risk profile records
        List<PerformanceStatus> blockingStates = List.of(PerformanceStatus.ACTIVE, PerformanceStatus.WATCH, PerformanceStatus.DOUBTFUL);
        if (accountRepository.existsByProfileIdAndLoanProductIdAndPerformanceStatusIn(request.getProfileId(), request.getLoanProductId(), blockingStates)) {
            throw new BusinessRuleViolationException("Profile already holds an unresolved active loan position for this product code.");
        }

        // 3. Stage 1 Mutation: Save initial footprint in DRAFT status
        LoanAccount account = new LoanAccount();
        account.setId("acc_" + UUID.randomUUID().toString());
        account.setProfileId(request.getProfileId());
        account.setLoanProductId(request.getLoanProductId());
        account.setIdempotencyKey(request.getIdempotencyKey());
        account.setInitialPrincipal(request.getInitialPrincipal());
        account.setIssuanceStatus(IssuanceStatus.DRAFT);
        account.setParentLoanAccountId(request.getParentLoanAccountId());
        account.setCreatedAt(ZonedDateTime.now());
        account.setUpdatedAt(ZonedDateTime.now());

        LoanAccount savedDraft = accountRepository.save(account);
        writeAuditLog(savedDraft.getId(), "DRAWDOWN_INITIATED", null, savedDraft, null, actor);

        // 4. Publish Decoupled Framework Event Outbound (To be caught by your system's messaging backbone)
        DraftLoanEvent draftEvent = new DraftLoanEvent(
                this,
                savedDraft.getId(),
                savedDraft.getProfileId(),
                savedDraft.getLoanProductId(),
                savedDraft.getInitialPrincipal(),
                savedDraft.getCreatedAt(),
                actor
        );
        eventPublisher.publishEvent(draftEvent);
        log.info("Draft loan footprint event broadcast outside module context for account: {}", savedDraft.getId());

        return convertToResponse(savedDraft);
    }

    /**
     * Async Inbound State Change Process driven purely by incoming evaluation events.
     */
    @Transactional
    public void processUnderwritingOutcome(LoanCreditScoreEvaluatedEvent evaluationEvent) {
        LoanAccount account = accountRepository.findById(evaluationEvent.getLoanAccountId())
                .orElse(null);

        if (account == null) {
            log.error("Fatal: Received underwriting evaluation results for an unrecognizable loan account identifier: {}", evaluationEvent.getLoanAccountId());
            return;
        }

        if (account.getIssuanceStatus() != IssuanceStatus.DRAFT) {
            log.warn("Lifecycle guard execution skipped. Target account asset is no longer in DRAFT status: {}", account.getId());
            return;
        }

        LoanAccount previousSnapshot = cloneState(account);

        if (evaluationEvent.getCreditLimitAllocated() != null) {
            account.setCreditLimitAtCapture(evaluationEvent.getCreditLimitAllocated().intValue());
        }
        account.setUpdatedAt(ZonedDateTime.now());

        // Evaluation Policy Check Rules
        boolean isApproved = "APPROVED".equalsIgnoreCase(evaluationEvent.getDecisionOutcome());
        boolean hasInsufficientLimit = isApproved && (evaluationEvent.getCreditLimitAllocated() == null ||
                account.getInitialPrincipal().compareTo(evaluationEvent.getCreditLimitAllocated()) > 0);

        if (!isApproved || hasInsufficientLimit) {
            account.setIssuanceStatus(IssuanceStatus.DENIED);
            LoanAccount deniedAccount = accountRepository.save(account);

            String eventReason = !isApproved ? "CREDIT_CHECK_FAILED" : "CREDIT_LIMIT_EXCEEDED";
            writeAuditLog(deniedAccount.getId(), eventReason, previousSnapshot, deniedAccount, evaluationEvent.getDecisionId(), evaluationEvent.getActor());
            log.info("Loan application pipeline process completed as DENIED for ID: {}. Reason: {}", deniedAccount.getId(), eventReason);
            return;
        }

        // Success Path Execution
        account.setIssuanceStatus(IssuanceStatus.APPROVED_ISSUED);
        account.setPerformanceStatus(PerformanceStatus.ACTIVE);
        account.setAccountNumber("LN-" + ZonedDateTime.now().getYear() + "-" + (ThreadLocalRandom.current().nextInt(89999) + 10000));
        account.setTakenAt(ZonedDateTime.now());

        LoanAccount issuedAccount = accountRepository.save(account);
        writeAuditLog(issuedAccount.getId(), "CREDIT_CHECK_PASSED", previousSnapshot, issuedAccount, evaluationEvent.getDecisionId(), evaluationEvent.getActor());
        log.info("Ledger records updated cleanly to APPROVED_ISSUED for Account Number: {}", issuedAccount.getAccountNumber());

        // Publish internal success notification indicating successful onboarding execution loops
        LoanApprovedStatusEvent approvedEvent = new LoanApprovedStatusEvent(
                this,
                issuedAccount.getId(),
                issuedAccount.getProfileId(),
                issuedAccount.getLoanProductId(),
                issuedAccount.getInitialPrincipal()
        );
        eventPublisher.publishEvent(approvedEvent);
    }

    @Transactional
    public LoanAccountResponseDto modifyPerformanceStatus(String accountId, StatusModificationRequestDto request, String actor) {
        LoanAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Target loan account reference identifier not discovered."));

        if (account.getIssuanceStatus() != IssuanceStatus.APPROVED_ISSUED) {
            throw new BusinessRuleViolationException("Cannot change the performance status of an unissued or denied loan account record line.");
        }

        LoanAccount snapshotBefore = cloneState(account);
        account.setPerformanceStatus(request.getTargetStatus());
        account.setUpdatedAt(ZonedDateTime.now());

        LoanAccount modifiedEntity = accountRepository.save(account);
        writeAuditLog(modifiedEntity.getId(), "PERFORMANCE_STATUS_CHANGED", snapshotBefore, modifiedEntity, null, actor);

        return convertToResponse(modifiedEntity);
    }

    @Transactional(readOnly = true)
    public Page<LoanAccountResponseDto> fetchLedgerSlice(String profileId, PerformanceStatus status, Pageable pageable) {
        return accountRepository.findAllLedgerSlice(profileId, status, pageable).map(this::convertToResponse);
    }

    private void writeAuditLog(String accountId, String event, Object before, Object after, String decisionId, String actor) {
        try {
            LoanAccountAuditLog logRow = new LoanAccountAuditLog();
            logRow.setId("log_" + UUID.randomUUID().toString());
            logRow.setLoanAccountId(accountId);
            logRow.setEventType(event);
            logRow.setPreviousState(before == null ? null : objectMapper.writeValueAsString(before));
            logRow.setNewState(objectMapper.writeValueAsString(after));
            logRow.setCreditScoreDecisionId(decisionId);
            logRow.setModifiedBy(actor);
            logRow.setRecordedAt(ZonedDateTime.now());
            auditLogRepository.save(logRow);
        } catch (Exception ex) {
            log.error("Fatal exception during audit log trace persistence compilation: ", ex);
        }
    }

    private LoanAccount cloneState(LoanAccount target) {
        LoanAccount clone = new LoanAccount();
        clone.setId(target.getId());
        clone.setIssuanceStatus(target.getIssuanceStatus());
        clone.setPerformanceStatus(target.getPerformanceStatus());
        clone.setCreditLimitAtCapture(target.getCreditLimitAtCapture());
        return clone;
    }

    private LoanAccountResponseDto convertToResponse(LoanAccount entity) {
        LoanAccountResponseDto dto = new LoanAccountResponseDto();
        dto.setId(entity.getId());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setProfileId(entity.getProfileId());
        dto.setLoanProductId(entity.getLoanProductId());
        dto.setIdempotencyKey(entity.getIdempotencyKey());
        dto.setInitialPrincipal(entity.getInitialPrincipal());
        dto.setCreditLimitAtCapture(entity.getCreditLimitAtCapture());
        dto.setIssuanceStatus(entity.getIssuanceStatus());
        dto.setPerformanceStatus(entity.getPerformanceStatus());
        dto.setParentLoanAccountId(entity.getParentLoanAccountId());
        dto.setTakenAt(entity.getTakenAt());
        return dto;
    }
}
