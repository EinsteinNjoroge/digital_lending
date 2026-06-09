package com.digital.lending.loanaccount.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "loan_account_audit_logs")
public class LoanAccountAuditLog {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "loan_account_id", nullable = false, length = 50)
    private String loanAccountId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "previous_state", columnDefinition = "TEXT")
    private String previousState;

    @Column(name = "new_state", nullable = false, columnDefinition = "TEXT")
    private String newState;

    @Column(name = "credit_score_decision_id", length = 100)
    private String creditScoreDecisionId;

    @Column(name = "modified_by", nullable = false, length = 100)
    private String modifiedBy;

    @Column(name = "recorded_at", nullable = false)
    private ZonedDateTime recordedAt = ZonedDateTime.now();
}