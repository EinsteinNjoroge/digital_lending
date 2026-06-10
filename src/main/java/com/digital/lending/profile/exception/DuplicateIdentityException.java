package com.digital.lending.profile.exception;

public class DuplicateIdentityException extends ProfileDomainException {
    public DuplicateIdentityException(String field, String value) {
        super("DUPLICATE_" + field.toUpperCase(), "The provided " + field + " '" + value + "' is already registered.");
    }
}
