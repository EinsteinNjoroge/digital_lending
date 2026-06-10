package com.digital.lending.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Payload envelope containing template codes and variable maps to issue notifications")
public class NotificationDispatchRequestDto {

    @NotBlank
    @Schema(example = "LOAN_DISBURSED_EMAIL", description = "Target template layout unique identifier")
    private String templateId;

    @NotBlank
    @Schema(example = "client.email@example.com", description = "Target delivery vector address or cellular routing endpoint line")
    private String destination;

    @NotNull
    @Schema(example = "{\"recipientName\":\"John Doe\", \"amount\":\"15000.00\", \"currency\":\"KES\", \"accountReference\":\"LN-00192\"}")
    private Map<String, String> templateVariables;

    @NotBlank
    @Schema(example = "LendingServiceEngine", description = "System boundary domain context identity issuing the notification push")
    private String actor;
}
