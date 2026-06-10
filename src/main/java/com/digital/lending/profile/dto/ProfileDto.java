package com.digital.lending.profile.dto;

import java.time.Instant;
import java.util.List;

public record ProfileDto(
    String id,
    String profileType,
    String email,
    String phoneCountryCode,
    String phoneNationalNumber,
    String residenceCountry,
    String status,
    String displayName,
    List<IdentityDto> identities,
    Instant createdAt
) {}
