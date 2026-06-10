package com.digital.lending.loanaccount.dto;

import com.digital.lending.loanaccount.enums.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
@Schema(name = "StatusModificationRequest", description = "Administrative operational command envelope to modify credit status parameters.")
public class StatusModificationRequestDto {
    @NotNull(message = "Target operational performance status state is required")
    @Schema(example = "WATCH", description = "Target lifecycle categorization phase")
    private PerformanceStatus targetStatus;

    @Schema(example = "System routine background cron detected DPD 30+ violation threshold frames.", description = "Operational justification narrative")
    private String updateReason;
}