package com.digital.lending.loanaccount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
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
@Schema(name = "LoanAccountResponse", description = "Complete ledger view data snapshot reflecting execution statuses.")
public class LoanAccountResponseDto {
    @Schema(example = "acc_882c4491-1c20-4e0c-bc1a-592b68f275ca")
    private String id;

    @Schema(example = "LN-2026-08123", description = "Null while processing in DRAFT state. Set upon successful underwriting completion.")
    private String accountNumber;

    @Schema(example = "CUST-254700112233")
    private String profileId;

    @Schema(example = "f186e626-c8b0-4e0c-bc1a-592b68f275ca")
    private String loanProductId;

    @Schema(example = "idem_tx_saf_8812")
    private String idempotencyKey;

    @Schema(example = "5000.00")
    private BigDecimal initialPrincipal;

    @Schema(example = "6750", description = "The formal credit boundary cap captured from underwriter scorecard response evaluations.")
    private Integer creditLimitAtCapture;

    @Schema(example = "DRAFT", description = "Current underwriting lifecycle phase: DRAFT, APPROVED_ISSUED, DENIED")
    private IssuanceStatus issuanceStatus;

    @Schema(example = "ACTIVE", description = "Financial performance classification bucket. Null if account is still a DRAFT or DENIED.")
    private PerformanceStatus performanceStatus;

    @Schema(example = "acc_771b3547-5c64-42ea-aee1-39930222d111")
    private String parentLoanAccountId;

    private ZonedDateTime takenAt;
}