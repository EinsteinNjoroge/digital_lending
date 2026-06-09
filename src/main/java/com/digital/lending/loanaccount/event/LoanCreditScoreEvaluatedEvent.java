package com.digital.lending.loanaccount.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;

@Getter
@ToString
public class LoanCreditScoreEvaluatedEvent extends ApplicationEvent {

    private final String loanAccountId;
    private final String decisionId;
    private final String decisionOutcome;
    private final BigDecimal creditLimitAllocated;
    private final Double scoreCalculated;
    private final String actor;

    public LoanCreditScoreEvaluatedEvent(Object source, String loanAccountId, String decisionId, String decisionOutcome, BigDecimal creditLimitAllocated, Double scoreCalculated, String actor) {
        super(source);
        this.loanAccountId = loanAccountId;
        this.decisionId = decisionId;
        this.decisionOutcome = decisionOutcome;
        this.creditLimitAllocated = creditLimitAllocated;
        this.scoreCalculated = scoreCalculated;
        this.actor = actor;
    }
}