package com.digital.lending.profile.exception;

import lombok.Getter;

@Getter
public abstract class ProfileDomainException extends RuntimeException {
    private final String errorCode;

    protected ProfileDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
