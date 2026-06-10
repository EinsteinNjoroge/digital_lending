package com.digital.lending.payment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_channels")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentChannel {

    @Id
    private String id;

    private String name;

    private Instant createdAt;
}
