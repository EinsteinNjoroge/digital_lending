package com.digital.lending.events;

import java.time.Instant;

public record ProfileRegisteredEvent(
        String profileId,
        String profileType,
        String displayName,
        String email,
        String phone,
        String residenceCountry,
        Instant occurredAt
) {}
