package com.digital.lending.loanaccount.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@ToString
public class DraftLoanEvent extends ApplicationEvent {

    private final String loanAccountId;
    private final String profileId;
    private final String loanProductId;
    private final BigDecimal initialPrincipal;
    private final ZonedDateTime createdAt;
    private final String actor;

    public DraftLoanEvent(Object source, String loanAccountId, String profileId, String loanProductId, BigDecimal initialPrincipal, ZonedDateTime createdAt, String actor) {
        super(source);
        this.loanAccountId = loanAccountId;
        this.profileId = profileId;
        this.loanProductId = loanProductId;
        this.initialPrincipal = initialPrincipal;
        this.createdAt = createdAt;
        this.actor = actor;
    }
}