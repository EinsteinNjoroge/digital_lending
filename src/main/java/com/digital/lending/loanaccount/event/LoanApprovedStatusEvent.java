package com.digital.lending.loanaccount.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
@ToString
public class LoanApprovedStatusEvent extends ApplicationEvent {

    private final String loanAccountId;
    private final String profileId;
    private final String loanProductId;
    private final BigDecimal principal;

    public LoanApprovedStatusEvent(Object source, String loanAccountId, String profileId, String loanProductId, BigDecimal principal) {
        super(source);
        this.loanAccountId = loanAccountId;
        this.profileId = profileId;
        this.loanProductId = loanProductId;
        this.principal = principal;
    }
}