package com.digital.lending.events;

import java.time.Instant;

public record InitialCreditProfileCreatedEvent(
        String profileId,
        String profileType,
        Instant occurredAt
) {}
