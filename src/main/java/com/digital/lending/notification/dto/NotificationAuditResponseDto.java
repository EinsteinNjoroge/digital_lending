package com.digital.lending.notification.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Detailed presentation block for an historically executed notification trace record line")
public record NotificationAuditResponseDto(
        String id,
        String templateId,
        String channelId,
        String recipientDestination,
        String resolvedTitle,
        String resolvedBody,
        String status,
        String errorMessage,
        String triggeredBy,
        LocalDateTime createdAt
) {}
