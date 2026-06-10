package com.digital.lending.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Standard system transactional snapshot output data line details")
public record PaymentResponseDto(
        @Schema(example = "tx_99201882") String id,
        @Schema(example = "REPAYMENT") String category,
        @Schema(example = "MPESA") String provider,
        @Schema(example = "COMPLETED") String status,
        @Schema(example = "LN-2026-99102") String accountReference,
        @Schema(example = "7500.00") BigDecimal amount,
        @Schema(example = "KES") String currency,
        @Schema(example = "MPESA-XYZ-99182") String externalReferenceNumber,
        LocalDateTime completedAt
) {}
