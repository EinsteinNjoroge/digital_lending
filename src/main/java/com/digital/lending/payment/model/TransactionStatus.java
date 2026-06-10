package com.digital.lending.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_transaction_statuses")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatus {

    @Id
    private String id;

    private String description;

    private Instant createdAt;
}
