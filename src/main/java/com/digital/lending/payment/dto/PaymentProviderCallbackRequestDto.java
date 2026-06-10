package com.digital.lending.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Provider callback payload used to finalize asynchronous payment processing")
public class PaymentProviderCallbackRequestDto {

    @Schema(example = "tx_99201882", description = "Internal payment transaction id, when known")
    private String internalTransactionId;

    @Schema(example = "MPESAREF1234ABCD", description = "Provider external reference number")
    private String externalReferenceNumber;

    @Schema(example = "MPESATX1234ABCD", description = "Provider-native transaction id")
    private String providerTransactionId;

    @NotBlank
    @Schema(example = "COMPLETED", description = "Terminal provider outcome status: COMPLETED or FAILED")
    private String outcomeStatus;

    @Schema(example = "LN-2026-99102", description = "Loan account reference, required when a repayment callback creates a transaction on the fly")
    private String accountReference;

    @Schema(example = "PROF-10029", description = "Profile id associated with the payment, optional but recommended")
    private String profileId;

    @Schema(example = "REPAYMENT", description = "Payment category, required when creating a repayment transaction from callback")
    private String categoryId;

    @NotNull
    @Schema(example = "7500.00")
    private BigDecimal amount;

    @NotBlank
    @Schema(example = "KES")
    private String currency;

    @Schema(example = "Insufficient funds", description = "Failure reason from the provider when outcomeStatus is FAILED")
    private String failureReason;

    @Schema(example = "{\"provider\":\"MPESA\",\"status\":\"COMPLETED\"}")
    private String rawPayload;

    @NotNull
    @Schema(example = "2026-06-10T10:30:00")
    private LocalDateTime callbackTimestamp;
}
