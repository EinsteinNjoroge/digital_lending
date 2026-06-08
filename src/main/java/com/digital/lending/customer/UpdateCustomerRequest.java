package com.digital.lending.customer;

public record UpdateCustomerRequest(
        String email,
        String phoneCountryCode,
        String phoneNationalNumber,
        String residenceCountry
) {}
