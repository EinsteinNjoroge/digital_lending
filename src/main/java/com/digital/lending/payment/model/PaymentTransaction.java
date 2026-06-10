package com.digital.lending.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
public class PaymentTransaction {
    @Id
    private String id;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "status_id")
    private String statusId;

    @Column(name = "account_reference")
    private String accountReference;

    @Column(name = "loan_account_id")
    private String loanAccountId;

    @Column(name = "profile_id")
    private String profileId;

    @Column(name = "sender_party_id")
    private String senderPartyId;

    @Column(name = "receiver_party_id")
    private String receiverPartyId;

    private BigDecimal amount;
    private String currency;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
