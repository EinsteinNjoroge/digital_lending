package com.digital.lending.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(value = "com.digital.lending.customer", name = "customerExceptionHandler")
public class GlobalExceptionHandler {

    /**
     * Handle business rules exceptions.
     */
    @ExceptionHandler(CustomerDomainException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomerDomainExceptions(CustomerDomainException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                null,
                Instant.now()
        );

        HttpStatus status = ex instanceof DuplicateIdentityException
                ? HttpStatus.CONFLICT
                : HttpStatus.UNPROCESSABLE_ENTITY;

        return ResponseEntity.status(status).body(error);
    }

    /**
     * 2. Handles validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage())
        );

        ApiErrorResponse error = new ApiErrorResponse(
                "INVALID_INPUT",
                "The request payload contains validation errors.",
                details,
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 2. Handles invalid input errors.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {

        String userFriendlyMessage = "Invalid input format. Check your enum values (e.g., customerType, documentType).";

        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                userFriendlyMessage = String.format("The value '%s' is invalid. Accepted values are: %s",
                        ife.getValue(),
                        java.util.Arrays.toString(ife.getTargetType().getEnumConstants()));
            }
        }

        ApiErrorResponse error = new ApiErrorResponse(
                "INVALID_INPUT_FORMAT",
                userFriendlyMessage,
                null,
                java.time.Instant.now()
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Securely handle database errors
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedDatabaseConstraints(org.springframework.dao.DataIntegrityViolationException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
                "DATA_INTEGRITY_CONFLICT",
                "The request could not be completed due to a low-level data consistency conflict.",
                null,
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * 500 Internal Server Errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericFallback(Exception ex) {
        ApiErrorResponse error = new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred on our systems. Please contact support if this persists.",
                null,
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}