package com.digital.lending.loanaccount.service;

import com.digital.lending.events.LoanAccountExposureChangedEvent;
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
import com.digital.lending.loanaccount.repository.LoanProductConfigurationProjectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanAccountManagementService {

    private static final String DEFAULT_DISBURSAL_PROVIDER = "INTERNAL";
    private static final String PAYMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String PAYMENT_CATEGORY_DISBURSEMENT = "DISBURSEMENT";
    private static final String PAYMENT_CATEGORY_REPAYMENT = "REPAYMENT";
    private static final String PAYMENT_EVENT_LISTENER_ACTOR = "payment-event-listener";

    private static final int DEFAULT_REPAYMENT_DUE_DAYS = 30;

    private final LoanAccountRepository accountRepository;
    private final LoanAccountAuditLogRepository auditLogRepository;
    private final LoanProductConfigurationProjectionRepository loanProductConfigurationProjectionRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanAccountResponseDto provisionNewAccount(LoanAccountOpeningRequestDto request, String actor) {
        Optional<LoanAccount> existingHolder = accountRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingHolder.isPresent()) {
            return convertToResponse(existingHolder.get());
        }

        List<PerformanceStatus> blockingStates = List.of(PerformanceStatus.ACTIVE, PerformanceStatus.WATCH, PerformanceStatus.DOUBTFUL);
        if (accountRepository.existsByProfileIdAndLoanProductIdAndPerformanceStatusIn(request.getProfileId(), request.getLoanProductId(), blockingStates)) {
            throw new BusinessRuleViolationException("Profile already holds an unresolved active loan position for this product code.");
        }

        LoanAccount account = new LoanAccount();
        account.setId("acc_" + UUID.randomUUID());
        account.setProfileId(request.getProfileId());
        account.setLoanProductId(request.getLoanProductId());
        account.setIdempotencyKey(request.getIdempotencyKey());
        account.setInitialPrincipal(request.getInitialPrincipal());
        account.setOutstandingPrincipal(request.getInitialPrincipal());
        account.setIssuanceStatus(IssuanceStatus.PENDING_SCORE_VALIDATION);
        account.setParentLoanAccountId(request.getParentLoanAccountId());
        account.setDaysPastDue(0);
        account.setCreatedAt(ZonedDateTime.now());
        account.setUpdatedAt(ZonedDateTime.now());

        LoanAccount savedDraft = accountRepository.save(account);
        writeAuditLog(savedDraft.getId(), "LOAN_APPLICATION_CREATED", null, savedDraft, null, actor);

        eventPublisher.publishEvent(new LoanApplicationCreatedEvent(
                savedDraft.getId(),
                savedDraft.getProfileId(),
                savedDraft.getLoanProductId(),
                savedDraft.getInitialPrincipal(),
                request.getPartnerId(),
                request.getCurrency(),
                request.getScoringFeatures() == null ? Map.of() : Map.copyOf(request.getScoringFeatures()),
                request.getDisbursementProviderId(),
                request.getDisbursementDestinationReference(),
                actor,
                savedDraft.getCreatedAt()
        ));
        log.info("Published LoanApplicationCreatedEvent for account {}", savedDraft.getId());

        return convertToResponse(savedDraft);
    }

    @Transactional
    public void processApprovedApplication(LoanApplicationApprovedEvent approvalEvent) {
        LoanAccount account = accountRepository.findById(approvalEvent.loanAccountId()).orElse(null);
        if (account == null) {
            log.error("Received approval for unknown loan account {}", approvalEvent.loanAccountId());
            return;
        }

        if (account.getIssuanceStatus() != IssuanceStatus.PENDING_SCORE_VALIDATION) {
            log.warn("Skipping approval processing for account {} because it is no longer awaiting score validation", approvalEvent.loanAccountId());
            return;
        }

        LoanAccount previousSnapshot = cloneState(account);
        if (approvalEvent.approvedLimit() != null) {
            account.setCreditLimitAtCapture(approvalEvent.approvedLimit().intValue());
        }

        boolean hasInsufficientLimit = approvalEvent.approvedLimit() == null
                || account.getInitialPrincipal().compareTo(approvalEvent.approvedLimit()) > 0;
        if (hasInsufficientLimit) {
            account.setIssuanceStatus(IssuanceStatus.DENIED);
            account.setUpdatedAt(ZonedDateTime.now());
            LoanAccount deniedAccount = accountRepository.save(account);
            writeAuditLog(deniedAccount.getId(), "CREDIT_LIMIT_EXCEEDED", previousSnapshot, deniedAccount, approvalEvent.decisionId(), approvalEvent.actor());
            log.info("Loan application {} denied because approved limit {} is lower than requested principal {}",
                    deniedAccount.getId(), approvalEvent.approvedLimit(), account.getInitialPrincipal());
            return;
        }

        account.setIssuanceStatus(IssuanceStatus.APPROVED);
        account.setPerformanceStatus(PerformanceStatus.ACTIVE);
        account.setAccountNumber("LN-" + ZonedDateTime.now().getYear() + "-" + (ThreadLocalRandom.current().nextInt(89999) + 10000));
        account.setUpdatedAt(ZonedDateTime.now());

        LoanAccount approvedAccount = accountRepository.save(account);
        writeAuditLog(approvedAccount.getId(), "LOAN_APPLICATION_APPROVED", previousSnapshot, approvedAccount, approvalEvent.decisionId(), approvalEvent.actor());
        publishExposureChangedEvent(approvedAccount, IssuanceStatus.APPROVED, approvedAccount.getOutstandingPrincipal(), ZonedDateTime.now());

        eventPublisher.publishEvent(new LoanDisbursalRequestedEvent(
                approvedAccount.getId(),
                approvedAccount.getAccountNumber(),
                approvedAccount.getProfileId(),
                approvedAccount.getLoanProductId(),
                approvedAccount.getInitialPrincipal(),
                approvalEvent.currency(),
                approvalEvent.disbursementProviderId() == null || approvalEvent.disbursementProviderId().isBlank()
                        ? DEFAULT_DISBURSAL_PROVIDER
                        : approvalEvent.disbursementProviderId(),
                approvalEvent.disbursementDestinationReference() == null || approvalEvent.disbursementDestinationReference().isBlank()
                        ? approvedAccount.getProfileId()
                        : approvalEvent.disbursementDestinationReference(),
                approvalEvent.actor(),
                ZonedDateTime.now()
        ));
        log.info("Published LoanDisbursalRequestedEvent for account {}", approvedAccount.getId());
    }

    @Transactional
    public void processRejectedApplication(LoanApplicationRejectedEvent rejectionEvent) {
        LoanAccount account = accountRepository.findById(rejectionEvent.loanAccountId()).orElse(null);
        if (account == null) {
            log.error("Received rejection for unknown loan account {}", rejectionEvent.loanAccountId());
            return;
        }

        if (account.getIssuanceStatus() != IssuanceStatus.PENDING_SCORE_VALIDATION) {
            log.warn("Skipping rejection processing for account {} because it is no longer awaiting score validation", rejectionEvent.loanAccountId());
            return;
        }

        LoanAccount previousSnapshot = cloneState(account);
        account.setIssuanceStatus(IssuanceStatus.DENIED);
        account.setUpdatedAt(ZonedDateTime.now());

        LoanAccount deniedAccount = accountRepository.save(account);
        String reason = rejectionEvent.rejectionReason() == null || rejectionEvent.rejectionReason().isBlank()
                ? rejectionEvent.decisionOutcome()
                : rejectionEvent.rejectionReason();
        writeAuditLog(deniedAccount.getId(), "LOAN_APPLICATION_REJECTED", previousSnapshot, deniedAccount, rejectionEvent.decisionId(), rejectionEvent.actor());
        log.info("Loan application {} was denied: {}", deniedAccount.getId(), reason);
    }

    @Transactional
    public void processPaymentEvent(PaymentEvent paymentEvent) {
        if (!PAYMENT_STATUS_COMPLETED.equalsIgnoreCase(paymentEvent.statusId())) {
            return;
        }

        LoanAccount account = accountRepository.findByAccountNumber(paymentEvent.accountReference()).orElse(null);
        if (account == null) {
            log.warn("No loan account found for payment event account reference {}", paymentEvent.accountReference());
            return;
        }

        if (PAYMENT_CATEGORY_DISBURSEMENT.equalsIgnoreCase(paymentEvent.categoryId())) {
            activateApprovedLoan(account, paymentEvent);
            return;
        }

        if (PAYMENT_CATEGORY_REPAYMENT.equalsIgnoreCase(paymentEvent.categoryId())) {
            applyRepayment(account, paymentEvent);
        }
    }

    private void activateApprovedLoan(LoanAccount account, PaymentEvent paymentEvent) {
        if (account.getIssuanceStatus() != IssuanceStatus.APPROVED) {
            return;
        }

        LoanAccount snapshotBefore = cloneState(account);
        ZonedDateTime activationTime = ZonedDateTime.now();
        account.setIssuanceStatus(IssuanceStatus.ACTIVE);
        account.setTakenAt(activationTime);
        account.setRepaymentDueAt(resolveRepaymentDueAt(account.getLoanProductId(), activationTime));
        account.setDaysPastDue(0);
        account.setLastServicedAt(activationTime);
        account.setUpdatedAt(activationTime);
        LoanAccount activated = accountRepository.save(account);
        writeAuditLog(activated.getId(), "LOAN_DISBURSED", snapshotBefore, activated, paymentEvent.transactionId(), PAYMENT_EVENT_LISTENER_ACTOR);
        publishExposureChangedEvent(activated, IssuanceStatus.ACTIVE, activated.getOutstandingPrincipal(), activationTime);
    }

    private void applyRepayment(LoanAccount account, PaymentEvent paymentEvent) {
        if (account.getIssuanceStatus() != IssuanceStatus.ACTIVE && account.getIssuanceStatus() != IssuanceStatus.SETTLED) {
            return;
        }

        LoanAccount snapshotBefore = cloneState(account);
        BigDecimal currentOutstanding = account.getOutstandingPrincipal() == null ? account.getInitialPrincipal() : account.getOutstandingPrincipal();
        BigDecimal updatedOutstanding = currentOutstanding.subtract(paymentEvent.amount());
        if (updatedOutstanding.compareTo(BigDecimal.ZERO) < 0) {
            updatedOutstanding = BigDecimal.ZERO;
        }
        account.setOutstandingPrincipal(updatedOutstanding);
        account.setUpdatedAt(ZonedDateTime.now());

        if (updatedOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            account.setIssuanceStatus(IssuanceStatus.SETTLED);
            account.setPerformanceStatus(PerformanceStatus.SETTLED);
            account.setSettledAt(ZonedDateTime.now());
            account.setRepaymentDueAt(null);
            account.setDaysPastDue(0);
        }

        LoanAccount saved = accountRepository.save(account);
        writeAuditLog(saved.getId(), "REPAYMENT_APPLIED", snapshotBefore, saved, paymentEvent.transactionId(), PAYMENT_EVENT_LISTENER_ACTOR);
        publishExposureChangedEvent(saved, saved.getIssuanceStatus(), saved.getOutstandingPrincipal(), ZonedDateTime.now());

        if (saved.getIssuanceStatus() == IssuanceStatus.SETTLED) {
            eventPublisher.publishEvent(new LoanAccountSettledEvent(
                    saved.getId(),
                    saved.getProfileId(),
                    saved.getAccountNumber(),
                    ZonedDateTime.now()
            ));
        }
    }

    @Transactional
    public LoanAccountResponseDto modifyPerformanceStatus(String accountId, StatusModificationRequestDto request, String actor) {
        LoanAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Target loan account reference identifier not discovered."));

        if (account.getIssuanceStatus() != IssuanceStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Cannot change the performance status of an unissued, denied, or settled loan account record line.");
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
            logRow.setId("log_" + UUID.randomUUID());
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
        clone.setAccountNumber(target.getAccountNumber());
        clone.setOutstandingPrincipal(target.getOutstandingPrincipal());
        clone.setSettledAt(target.getSettledAt());
        clone.setRepaymentDueAt(target.getRepaymentDueAt());
        clone.setDaysPastDue(target.getDaysPastDue());
        clone.setLastServicedAt(target.getLastServicedAt());
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
        dto.setOutstandingPrincipal(entity.getOutstandingPrincipal());
        dto.setCreditLimitAtCapture(entity.getCreditLimitAtCapture());
        dto.setIssuanceStatus(entity.getIssuanceStatus());
        dto.setPerformanceStatus(entity.getPerformanceStatus());
        dto.setParentLoanAccountId(entity.getParentLoanAccountId());
        dto.setTakenAt(entity.getTakenAt());
        dto.setSettledAt(entity.getSettledAt());
        dto.setRepaymentDueAt(entity.getRepaymentDueAt());
        dto.setDaysPastDue(entity.getDaysPastDue());
        dto.setLastServicedAt(entity.getLastServicedAt());
        return dto;
    }

    private ZonedDateTime resolveRepaymentDueAt(String loanProductId, ZonedDateTime activationTime) {
        return loanProductConfigurationProjectionRepository.findById(loanProductId)
                .map(projection -> projection.getRepaymentDueDays())
                .map(activationTime::plusDays)
                .orElse(activationTime.plusDays(DEFAULT_REPAYMENT_DUE_DAYS));
    }

    private void publishExposureChangedEvent(LoanAccount account, IssuanceStatus issuanceStatus, BigDecimal outstandingPrincipal, ZonedDateTime occurredAt) {
        eventPublisher.publishEvent(new LoanAccountExposureChangedEvent(
                account.getId(),
                account.getProfileId(),
                account.getAccountNumber(),
                outstandingPrincipal == null ? BigDecimal.ZERO : outstandingPrincipal,
                issuanceStatus.name(),
                occurredAt
        ));
    }
}
