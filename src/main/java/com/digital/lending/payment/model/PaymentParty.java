package com.digital.lending.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_parties")
@Getter
@Setter
public class PaymentParty {
    @Id
    private String id;

    @Column(name = "party_reference", unique = true)
    private String partyReference;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "party_type", nullable = false)
    private String partyType;

    @Column(name = "source_module", nullable = false)
    private String sourceModule;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
