package com.digital.lending.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Request to create or record a payment.")
public class PaymentExecutionRequestDto {

    @NotBlank
    @Schema(example = "idem-key-88192-332", description = "Idempotency key for safe retries.")
    private String idempotencyKey;

    @NotBlank
    @Schema(example = "REPAYMENT", description = "Target category identifier (e.g., DISBURSEMENT, REPAYMENT, REVERSAL)")
    private String categoryId;

    @NotBlank
    @Schema(example = "MPESA", description = "Target infrastructure platform route (e.g., MPESA, AIRTEL_MONEY, PESALINK)")
    private String providerId;

    @NotBlank
    @Schema(example = "LN-2026-99102", description = "Loan account reference shown to the customer or provider.")
    private String accountReference;

    @Schema(example = "acc_882c4491-1c20-4e0c-bc1a-592b68f275ca", description = "Optional internal loan account id for event-driven payment workflows")
    private String loanAccountId;

    @Schema(example = "PROF-10029", description = "Optional profile identifier associated with the transaction for downstream event consumers")
    private String profileId;

    @NotBlank
    @Schema(example = "PART-CUST-10029", description = "Sender profile pointer or corporate wallet token reference")
    private String senderPartyReference;

    @NotBlank
    @Schema(example = "PART-CO-DISBURSE-01", description = "Receiver profile pointer or escrow account line reference")
    private String receiverPartyReference;

    @NotNull
    @Positive
    @Schema(example = "7500.00", description = "Monetary precision balance to process")
    private BigDecimal amount;

    @NotBlank
    @Schema(example = "KES", description = "ISO alpha-3 currency code standard")
    private String currency;
}
