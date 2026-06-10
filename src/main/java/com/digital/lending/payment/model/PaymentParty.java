package com.digital.lending.payment.model;

import jakarta.persistence.*;
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
