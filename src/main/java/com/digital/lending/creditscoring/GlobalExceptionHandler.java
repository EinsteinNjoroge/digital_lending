package com.digital.lending.creditscoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(value = "com.digital.lending.creditscoring", name = "creditScoringExceptionHandler")
public class GlobalExceptionHandler {

    /**
     * Handle business logic validation constraints and multi-tenant coordinate collisions.
     * Maps onto HTTP 409 Conflict (ideal for duplicated/colliding state) or HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Business policy restriction triggered: {}", ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                "CONFLICTING_CONFIGURATION",
                ex.getMessage(),
                "Verify coordinates match an unmapped combination or perform a PUT request to modify the existing record layout mapping constraints.",
                ZonedDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle missing entity lookup mismatches.
     * Maps onto HTTP 404 Not Found.
     */
    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRecordNotFoundException(RecordNotFoundException ex) {
        log.warn("Target database record lookup failure: {}", ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                "RECORD_NOT_FOUND",
                ex.getMessage(),
                "The requested primary tracking key mapping coordinate branch does not exist inside our system registry profiles.",
                ZonedDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle controller schema request field body validation failures (e.g., missing @NotBlank or @NotNull fields).
     * Maps onto HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Inbound schema validation rejected payload parameters: {}", validationErrors);

        ApiErrorResponse error = new ApiErrorResponse(
                "BAD_REQUEST_PAYLOAD",
                "Schema verification constraints failed validation checks.",
                validationErrors,
                ZonedDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Catch-all fallback interceptor to cleanly wrap unmapped underlying exceptions.
     * Maps onto HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled runtime technical system fault captured by fallback routine", ex);

        ApiErrorResponse error = new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected technical systemic runtime variance occurred on our engineering backplane layers.",
                "Please capture this payload reference token string and report it directly onto support workflows.",
                ZonedDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}