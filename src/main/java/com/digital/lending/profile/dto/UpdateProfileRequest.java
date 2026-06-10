package com.digital.lending.profile.dto;

public record UpdateProfileRequest(
        String email,
        String phoneCountryCode,
        String phoneNationalNumber,
        String residenceCountry
) {}
