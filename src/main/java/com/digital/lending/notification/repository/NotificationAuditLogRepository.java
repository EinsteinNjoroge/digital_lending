package com.digital.lending.notification.repository;

import com.digital.lending.notification.model.NotificationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationAuditLogRepository extends
        JpaRepository<NotificationAuditLog, String>,
        JpaSpecificationExecutor<NotificationAuditLog> {}
