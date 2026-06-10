package com.digital.lending.notification.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(value = "com.digital.lending.notification", name = "notificationExceptionHandler")
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailures(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            if (err instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), err.getDefaultMessage());
            } else {
                errors.put(err.getObjectName(), err.getDefaultMessage());
            }
        });
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "BAD_REQUEST_PAYLOAD",
                "Validation failures detected.",
                errors,
                ZonedDateTime.now()
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedPayload(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "MALFORMED_REQUEST_PAYLOAD",
                "Request body is missing, malformed, or contains invalid JSON.",
                null,
                ZonedDateTime.now()
        ));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
            UnsupportedOperationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
        HttpStatus status = ex instanceof IllegalStateException ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_REQUEST;
        String code = ex instanceof IllegalStateException ? "BUSINESS_RULE_VIOLATION" : "INVALID_REQUEST";
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                code,
                ex.getMessage(),
                null,
                ZonedDateTime.now()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Notification data integrity collision detected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "DATA_INTEGRITY_COLLISION",
                "The request conflicts with existing notification data.",
                null,
                ZonedDateTime.now()
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                "RESOURCE_NOT_FOUND",
                "The requested endpoint or resource was not found.",
                null,
                ZonedDateTime.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternal(Exception ex) {
        log.error("Unhandled notification module exception intercepted: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An internal system error occurred while processing the notification request.",
                null,
                ZonedDateTime.now()
        ));
    }
}
