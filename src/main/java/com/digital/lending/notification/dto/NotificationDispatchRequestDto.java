package com.digital.lending.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Schema(description = "Request to send a notification from a stored template.")
public class NotificationDispatchRequestDto {

    @NotBlank
    @Schema(example = "LOAN_DISBURSED_EMAIL", description = "Target template layout unique identifier")
    private String templateId;

    @NotBlank
    @Schema(example = "client.email@example.com", description = "Recipient email address, phone number, or push token.")
    private String destination;

    @NotNull
    @Schema(example = "{\"recipientName\":\"John Doe\", \"amount\":\"15000.00\", \"currency\":\"KES\", \"accountReference\":\"LN-00192\"}")
    private Map<String, String> templateVariables;

    @NotBlank
    @Schema(example = "loan-servicing-job", description = "System user or process that triggered the notification.")
    private String actor;
}
