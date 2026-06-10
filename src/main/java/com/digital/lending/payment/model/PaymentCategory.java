package com.digital.lending.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_categories")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCategory {

    @Id
    private String id;

    private String name;

    private String description;

    private Instant createdAt;
}
