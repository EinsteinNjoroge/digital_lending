package com.digital.lending.customer.exception;

import lombok.Getter;

@Getter
public abstract class CustomerDomainException extends RuntimeException {
    private final String errorCode;

    protected CustomerDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}