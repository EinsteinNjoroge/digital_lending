package com.digital.lending.profile.exception;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, String> details,
        Instant timestamp
) {}
