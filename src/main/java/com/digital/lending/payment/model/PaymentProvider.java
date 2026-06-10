package com.digital.lending.payment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_providers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProvider {

    @Id
    private String id;

    private String channelId;

    private String name;

    private String isActive;

    private Instant createdAt;
}
