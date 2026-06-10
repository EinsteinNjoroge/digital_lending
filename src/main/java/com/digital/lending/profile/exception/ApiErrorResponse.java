package com.digital.lending.profile.exception;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String errorCode,
        String message,
        Map<String, String> details,
        Instant timestamp
) {}
