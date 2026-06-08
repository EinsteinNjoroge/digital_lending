package com.digital.lending.customer.dto;

public record UpdateCustomerRequest(
        String email,
        String phoneCountryCode,
        String phoneNationalNumber,
        String residenceCountry
) {}
