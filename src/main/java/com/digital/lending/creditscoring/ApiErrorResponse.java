package com.digital.lending.creditscoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiErrorResponse", description = "Standardized error wrapper returned when an execution layer constraint is violated")
public class ApiErrorResponse {
    private String code;
    private String message;
    private String details;
    private ZonedDateTime timestamp;
}