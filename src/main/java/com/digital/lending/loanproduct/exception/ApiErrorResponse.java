package com.digital.lending.loanproduct.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiErrorResponse", description = "Standard structure detailing internal pipeline validation or runtime failures.")
public class ApiErrorResponse {
    private String errorCode;
    private String message;
    private Map<String, String> details;
    private ZonedDateTime timestamp;
}