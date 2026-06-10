package com.digital.lending.notification.repository;

import com.digital.lending.notification.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
}
