package com.digital.lending.profile.exception;

public class IdentityRequiredException extends ProfileDomainException {
    public IdentityRequiredException() {
        super("IDENTITY_REQUIRED", "Individual profiles require either a valid National ID or a Passport Number.");
    }
}
