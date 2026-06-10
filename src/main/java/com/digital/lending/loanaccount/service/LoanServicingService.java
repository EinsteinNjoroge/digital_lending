package com.digital.lending.loanaccount.service;

import com.digital.lending.events.LoanAccountOverdueEvent;
import com.digital.lending.loanaccount.dto.LoanServicingRunResponseDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.model.LoanAccount;
import com.digital.lending.loanaccount.model.LoanAccountAuditLog;
import com.digital.lending.loanaccount.repository.LoanAccountAuditLogRepository;
import com.digital.lending.loanaccount.repository.LoanAccountRepository;
import com.digital.lending.loanaccount.repository.LoanProductConfigurationProjectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServicingService {

    private static final int WATCH_THRESHOLD_DAYS = 1;
    private static final int DOUBTFUL_THRESHOLD_DAYS = 180;
    private static final String SERVICING_ACTOR = "loan-servicing-job";

    private final LoanAccountRepository loanAccountRepository;
    private final LoanAccountAuditLogRepository loanAccountAuditLogRepository;
    private final LoanProductConfigurationProjectionRepository loanProductConfigurationProjectionRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanServicingRunResponseDto runServicing(String trigger) {
        ZonedDateTime now = ZonedDateTime.now();
        List<LoanAccount> candidateAccounts = loanAccountRepository.findServicingCandidates(
                List.of(IssuanceStatus.ACTIVE),
                List.of(PerformanceStatus.ACTIVE, PerformanceStatus.WATCH, PerformanceStatus.DOUBTFUL),
                now
        );

        int updatedAccounts = 0;
        for (LoanAccount loanAccount : candidateAccounts) {
            PerformanceStatus targetStatus = resolvePerformanceStatus(loanAccount.getRepaymentDueAt(), now);
            int daysPastDue = calculateDaysPastDue(loanAccount.getRepaymentDueAt(), now);
            boolean changed = applyServicingUpdate(loanAccount, targetStatus, daysPastDue, now);
            if (changed) {
                updatedAccounts++;
            }
        }

        return new LoanServicingRunResponseDto(
                candidateAccounts.size(),
                updatedAccounts,
                candidateAccounts.size(),
                now,
                trigger
        );
    }

    private boolean applyServicingUpdate(
            LoanAccount loanAccount,
            PerformanceStatus targetStatus,
            int daysPastDue,
            ZonedDateTime processedAt) {

        LoanAccount previousSnapshot = snapshot(loanAccount);
        PerformanceStatus previousStatus = loanAccount.getPerformanceStatus();
        int previousDaysPastDue = loanAccount.getDaysPastDue();

        loanAccount.setPerformanceStatus(targetStatus);
        loanAccount.setDaysPastDue(daysPastDue);
        loanAccount.setLastServicedAt(processedAt);
        loanAccount.setUpdatedAt(processedAt);
        LoanAccount savedAccount = loanAccountRepository.save(loanAccount);

        boolean statusChanged = previousStatus != targetStatus;
        boolean daysChanged = previousDaysPastDue != daysPastDue;

        if (statusChanged || daysChanged) {
            writeAuditLog(savedAccount, previousSnapshot);
            publishOverdueEventIfNeeded(savedAccount, targetStatus, daysPastDue, processedAt, statusChanged);
        }

        return statusChanged || daysChanged;
    }

    private void publishOverdueEventIfNeeded(
            LoanAccount loanAccount,
            PerformanceStatus targetStatus,
            int daysPastDue,
            ZonedDateTime occurredAt,
            boolean statusChanged) {

        if (!statusChanged || targetStatus == PerformanceStatus.SETTLED || daysPastDue < WATCH_THRESHOLD_DAYS) {
            return;
        }

        String currency = loanProductConfigurationProjectionRepository.findById(loanAccount.getLoanProductId())
                .map(projection -> projection.getCurrency())
                .orElse("KES");

        eventPublisher.publishEvent(new LoanAccountOverdueEvent(
                loanAccount.getId(),
                loanAccount.getProfileId(),
                loanAccount.getAccountNumber(),
                loanAccount.getOutstandingPrincipal(),
                currency,
                daysPastDue,
                targetStatus.name(),
                occurredAt
        ));

        log.info("Servicing marked loan account {} as {} ({} days past due)",
                loanAccount.getId(), targetStatus, daysPastDue);
    }

    private void writeAuditLog(LoanAccount savedAccount, LoanAccount previousSnapshot) {
        try {
            LoanAccountAuditLog auditLog = new LoanAccountAuditLog();
            auditLog.setId("log_" + UUID.randomUUID());
            auditLog.setLoanAccountId(savedAccount.getId());
            auditLog.setEventType("SERVICING_STATUS_UPDATED");
            auditLog.setPreviousState(objectMapper.writeValueAsString(previousSnapshot));
            auditLog.setNewState(objectMapper.writeValueAsString(savedAccount));
            auditLog.setModifiedBy(SERVICING_ACTOR);
            auditLog.setRecordedAt(ZonedDateTime.now());
            loanAccountAuditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to write servicing audit log for loan account {}", savedAccount.getId(), ex);
        }
    }

    private LoanAccount snapshot(LoanAccount loanAccount) {
        LoanAccount snapshot = new LoanAccount();
        snapshot.setId(loanAccount.getId());
        snapshot.setIssuanceStatus(loanAccount.getIssuanceStatus());
        snapshot.setPerformanceStatus(loanAccount.getPerformanceStatus());
        snapshot.setOutstandingPrincipal(loanAccount.getOutstandingPrincipal());
        snapshot.setRepaymentDueAt(loanAccount.getRepaymentDueAt());
        snapshot.setDaysPastDue(loanAccount.getDaysPastDue());
        snapshot.setLastServicedAt(loanAccount.getLastServicedAt());
        snapshot.setSettledAt(loanAccount.getSettledAt());
        return snapshot;
    }

    private PerformanceStatus resolvePerformanceStatus(ZonedDateTime repaymentDueAt, ZonedDateTime now) {
        int daysPastDue = calculateDaysPastDue(repaymentDueAt, now);
        if (daysPastDue >= DOUBTFUL_THRESHOLD_DAYS) {
            return PerformanceStatus.DOUBTFUL;
        }
        if (daysPastDue >= WATCH_THRESHOLD_DAYS) {
            return PerformanceStatus.WATCH;
        }
        return PerformanceStatus.ACTIVE;
    }

    private int calculateDaysPastDue(ZonedDateTime repaymentDueAt, ZonedDateTime now) {
        if (repaymentDueAt == null || repaymentDueAt.isAfter(now)) {
            return 0;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(repaymentDueAt.toLocalDate(), now.toLocalDate()));
    }
}
