package com.digital.lending.customer;

import java.time.Instant;
import java.util.List;

public record CustomerDto(
    String id,
    String customerType,
    String email,
    String phoneCountryCode,
    String phoneNationalNumber,
    String residenceCountry,
    String status,
    String displayName,
    List<IdentityDto> identities,
    Instant createdAt
) {}