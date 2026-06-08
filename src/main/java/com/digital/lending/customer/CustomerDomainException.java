package com.digital.lending.customer;

import lombok.Getter;

@Getter
public abstract class CustomerDomainException extends RuntimeException {
    private final String errorCode;

    protected CustomerDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}

class DuplicateIdentityException extends CustomerDomainException {
    public DuplicateIdentityException(String field, String value) {
        super("DUPLICATE_" + field.toUpperCase(), "The provided " + field + " '" + value + "' is already registered.");
    }
}

class IdentityRequiredException extends CustomerDomainException {
    public IdentityRequiredException() {
        super("IDENTITY_REQUIRED", "Individual accounts require either a valid National ID or a Passport Number.");
    }
}