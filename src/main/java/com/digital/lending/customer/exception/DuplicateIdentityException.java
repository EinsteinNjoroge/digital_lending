package com.digital.lending.customer.exception;

public class DuplicateIdentityException extends CustomerDomainException {
    public DuplicateIdentityException(String field, String value) {
        super("DUPLICATE_" + field.toUpperCase(), "The provided " + field + " '" + value + "' is already registered.");
    }
}