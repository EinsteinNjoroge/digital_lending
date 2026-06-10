package com.digital.lending.loanaccount.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "com.digital.lending.loanaccount.controller")
public class AccountsModuleExceptionHandler {

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleViolationException ex) {
        log.warn("Credit policy exception triggered: {}", ex.getMessage());
        ApiErrorResponse error = ApiErrorResponse.builder()
                .errorCode("BUSINESS_RULE_VIOLATION")
                .message(ex.getMessage())
                .timestamp(ZonedDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .errorCode("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(ZonedDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((err) -> {
            String fieldName = ((FieldError) err).getField();
            String errorMessage = err.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });

        ApiErrorResponse error = ApiErrorResponse.builder()
                .errorCode("BAD_REQUEST_PAYLOAD")
                .message("Input payload validation failed.")
                .timestamp(ZonedDateTime.now())
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Database constraint breach caught inside execution pool: {}", ex.getMessage());

        String cleanMessage = "Data conflict or database uniqueness constraint violation occurred.";
        if (ex.getMessage() != null && ex.getMessage().contains("uk_active_product_per_profile")) {
            cleanMessage = "Profile already has an active loan of this product type.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("uk_loan_account_accounts_idempotency")) {
            cleanMessage = "Duplicate idempotency key detected.";
        }

        ApiErrorResponse error = ApiErrorResponse.builder()
                .errorCode("DATA_CONFLICT_ERROR")
                .message(cleanMessage)
                .timestamp(ZonedDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleFallback(Exception ex) {
        log.error("Unhandled runtime execution fault captured: ", ex);
        ApiErrorResponse error = ApiErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_FAULT")
                .message("An unexpected internal error occurred.")
                .timestamp(ZonedDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
