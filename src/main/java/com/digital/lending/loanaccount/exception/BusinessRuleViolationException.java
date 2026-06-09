package com.digital.lending.loanaccount.exception;

public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) { super(message); }
}