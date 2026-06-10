package com.digital.lending.notification.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiErrorResponse", description = "Standard structure detailing validation, business, and runtime failures in the notification module.")
public class ApiErrorResponse {
    private String errorCode;
    private String message;
    private Map<String, String> details;
    private ZonedDateTime timestamp;
}
