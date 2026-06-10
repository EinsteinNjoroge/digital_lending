package com.digital.lending.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_audit_logs")
@Getter
@Setter
public class NotificationAuditLog {
    @Id
    private String id;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "recipient_destination")
    private String recipientDestination;

    @Column(name = "resolved_title")
    private String resolvedTitle;

    @Column(name = "resolved_body")
    private String resolvedBody;

    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
