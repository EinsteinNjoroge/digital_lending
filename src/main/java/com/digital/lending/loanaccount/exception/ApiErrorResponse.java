package com.digital.lending.loanaccount.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@Schema(name = "ApiErrorResponse", description = "Standardized error wrapper returned when a transaction or query block fails.")
public class ApiErrorResponse {
    @Schema(example = "BUSINESS_RULE_VIOLATION", description = "High-level categorized system error code token")
    private String errorCode;

    @Schema(example = "Profile Reference prof_ind_9921 already holds an active obligation under Product ID f186... Concurrent exposure denied.", description = "Human-readable root cause explanation")
    private String message;

    @Schema(example = "2026-06-09T15:53:18Z")
    private ZonedDateTime timestamp;

    @Schema(description = "Map containing granular field-level validation flags, if applicable")
    private Map<String, String> details;
}