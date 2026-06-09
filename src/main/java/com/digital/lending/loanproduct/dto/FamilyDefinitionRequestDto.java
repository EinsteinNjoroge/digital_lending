package com.digital.lending.loanproduct.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "FamilyDefinitionRequest", description = "Input payload required to provision or update a foundational loan family route framework.")
public class FamilyDefinitionRequestDto {

    @NotBlank(message = "Family code identifier is mandatory")
    @Size(max = 32)
    @Schema(description = "Unique alphanumeric token routing this family architecture type", example = "M-PESA_NANO_V1")
    private String familyCode;

    @NotBlank(message = "Display name is required")
    @Size(max = 100)
    @Schema(description = "User-facing descriptive title for internal back-office administration platforms", example = "Safaricom M-Pesa Consumer Micro-Lending Family")
    private String displayName;

    @NotBlank(message = "Disbursement handler token is required")
    @Size(max = 64)
    @Schema(description = "Spring Bean bean identifier or class target executing disbursement rails", example = "mpesaB2CDisbursementHandler")
    private String disbursementHandlerToken;

    @NotBlank(message = "Accrual calculation handler token is required")
    @Size(max = 64)
    @Schema(description = "Engine handler governing daily or compound interest amortization mechanics", example = "straightLineDailyAccrualHandler")
    private String accrualHandlerToken;

    @NotBlank(message = "Repayment clearing handler token is required")
    @Size(max = 64)
    @Schema(description = "Target mechanism resolving incoming ledger settlement balances", example = "fifoLedgerRepaymentSettlementHandler")
    private String repaymentHandlerToken;

    @NotBlank(message = "Delinquency pipeline state handler token is required")
    @Size(max = 64)
    @Schema(description = "System route handling state movements into overdue tracking pools", example = "automatedCrbSoftLockDelinquencyHandler")
    private String delinquencyHandlerToken;

    @NotNull(message = "Active operational status flag is required")
    @Schema(description = "Toggles runtime indexing capabilities for child product configuration inheritance cascades", example = "true")
    private Boolean isActive;
}