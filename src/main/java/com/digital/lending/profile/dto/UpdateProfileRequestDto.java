package com.digital.lending.profile.dto;

public record UpdateProfileRequestDto(
        String email,
        String phoneCountryCode,
        String phoneNationalNumber,
        String residenceCountry
) {}
