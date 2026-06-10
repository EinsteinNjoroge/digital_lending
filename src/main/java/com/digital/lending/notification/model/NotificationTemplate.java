package com.digital.lending.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
public class NotificationTemplate {
    @Id
    private String id;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "title_template")
    private String titleTemplate;

    @Column(name = "body_template")
    private String bodyTemplate;

    @Column(name = "is_active")
    private String isActive;
}
