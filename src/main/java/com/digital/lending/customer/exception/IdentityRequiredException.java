package com.digital.lending.customer.exception;

public class IdentityRequiredException extends CustomerDomainException {
    public IdentityRequiredException() {
        super("IDENTITY_REQUIRED", "Individual accounts require either a valid National ID or a Passport Number.");
    }
}