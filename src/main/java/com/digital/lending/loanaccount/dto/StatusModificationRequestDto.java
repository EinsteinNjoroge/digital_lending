package com.digital.lending.loanaccount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.digital.lending.loanaccount.dto.LoanAccountOpeningRequestDto;
import com.digital.lending.loanaccount.dto.LoanAccountResponseDto;
import com.digital.lending.loanaccount.dto.StatusModificationRequestDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.event.DraftLoanEvent;
import com.digital.lending.loanaccount.event.LoanApprovedStatusEvent;
import com.digital.lending.loanaccount.event.LoanCreditScoreEvaluatedEvent;
import com.digital.lending.loanaccount.exception.BusinessRuleViolationException;
import com.digital.lending.loanaccount.exception.ResourceNotFoundException;
import com.digital.lending.loanaccount.model.LoanAccount;
import com.digital.lending.loanaccount.model.LoanAccountAuditLog;
import com.digital.lending.loanaccount.repository.LoanAccountAuditLogRepository;
import com.digital.lending.loanaccount.repository.LoanAccountRepository;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;


@Data
@Schema(name = "StatusModificationRequest", description = "Administrative operational command envelope to modify credit status parameters.")
public class StatusModificationRequestDto {
    @NotNull(message = "Target operational performance status state is required")
    @Schema(example = "WATCH", description = "Target lifecycle categorization phase")
    private PerformanceStatus targetStatus;

    @Schema(example = "System routine background cron detected DPD 30+ violation threshold frames.", description = "Operational justification narrative")
    private String updateReason;
}