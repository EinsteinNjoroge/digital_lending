CREATE TABLE notification_channels (
    id VARCHAR(30) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE notification_templates (
    id VARCHAR(100) PRIMARY KEY,
    channel_id VARCHAR(30) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    is_active VARCHAR(5) NOT NULL DEFAULT 'TRUE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (channel_id) REFERENCES notification_channels(id)
);

CREATE TABLE notification_audit_logs (
    id VARCHAR(50) PRIMARY KEY,
    template_id VARCHAR(100) NOT NULL,
    channel_id VARCHAR(30) NOT NULL,
    recipient_destination VARCHAR(255) NOT NULL,
    resolved_title VARCHAR(255) NOT NULL,
    resolved_body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    triggered_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (template_id) REFERENCES notification_templates(id),
    FOREIGN KEY (channel_id) REFERENCES notification_channels(id)
);

CREATE INDEX idx_notif_audit_recipient
    ON notification_audit_logs(recipient_destination);

CREATE INDEX idx_notif_audit_template
    ON notification_audit_logs(template_id);

INSERT INTO notification_channels (id, description, created_at) VALUES
    ('EMAIL', 'Electronic Mail System Outbound', '2026-06-10 00:00:00'),
    ('SMS', 'Short Message Service Cellular Infrastructure', '2026-06-10 00:00:00'),
    ('PUSH', 'Mobile Application Push Notification Gateways', '2026-06-10 00:00:00');

INSERT INTO notification_templates (id, channel_id, title_template, body_template, created_at, updated_at) VALUES
    ('LOAN_DISBURSED_EMAIL', 'EMAIL', 'Your Loan Funds Have Been Disbursed!',
    '<!DOCTYPE html><html><body style="font-family: Arial, sans-serif; color: #333333; line-height: 1.6;"><div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;"><h2 style="color: #2e7d32;">Great news, {{recipientName}}!</h2><p>We have successfully processed your loan application. The requested funds have been transferred directly to your selected disbursement channel.</p><div style="background-color: #f9f9f9; padding: 15px; border-left: 4px solid #2e7d32; margin: 20px 0;"><strong>Disbursed Amount:</strong> {{currency}} {{amount}}<br><strong>Account Number:</strong> {{accountReference}}</div><p>Thank you for financing with us!</p><hr style="border: none; border-top: 1px solid #eeeeee;" /><p style="font-size: 12px; color: #777777;">This is an automated operational system message. Please do not reply directly.</p></div></body></html>',
    '2026-06-10 00:00:00',
'2026-06-10 00:00:00'),

    ('LOAN_DISBURSED_SMS', 'SMS', 'Loan Disbursed',
    'Hello {{recipientName}}, KES {{amount}} has been cleanly disbursed to your account {{accountReference}}. Thank you!',
    '2026-06-10 00:00:00',
    '2026-06-10 00:00:00'),

    ('REPAYMENT_RECEIVED_EMAIL', 'EMAIL', 'Payment Received - Thank You',
    '<!DOCTYPE html><html><body style="font-family: Arial, sans-serif; color: #333333; line-height: 1.6;"><div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;"><h2 style="color: #1565c0;">Payment Confirmed</h2><p>Dear {{recipientName}},</p><p>We are writing to confirm that we have safely received your recent loan installment payment execution run.</p><div style="background-color: #f9f9f9; padding: 15px; border-left: 4px solid #1565c0; margin: 20px 0;"><strong>Amount Paid:</strong> {{currency}} {{amount}}<br><strong>Target Account Reference:</strong> {{accountReference}}<br><strong>Value Date:</strong> {{valueDate}}</div><p>Your updated statement balance adjustments will reflect inside your mobile application profile timeline shortly.</p></div></body></html>',
    '2026-06-10 00:00:00',
'2026-06-10 00:00:00'),

    ('REPAYMENT_RECEIVED_SMS', 'SMS', 'Payment Confirmed',
    'Hello {{recipientName}}, we confirmed receipt of your payment of KES {{amount}} for loan {{accountReference}}.',
    '2026-06-10 00:00:00',
'2026-06-10 00:00:00'),

    ('LOAN_SETTLED_EMAIL', 'EMAIL', 'Loan Account Fully Settled',
    '<!DOCTYPE html><html><body style="font-family: Arial, sans-serif; color: #333333; line-height: 1.6;"><div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;"><h2 style="color: #2e7d32;">Congratulations, {{recipientName}}!</h2><p>Your loan account has now been fully settled.</p><div style="background-color: #f9f9f9; padding: 15px; border-left: 4px solid #2e7d32; margin: 20px 0;"><strong>Account Reference:</strong> {{accountReference}}<br><strong>Settlement Date:</strong> {{settlementDate}}</div><p>Thank you for honoring your repayment obligations.</p></div></body></html>',
    '2026-06-10 00:00:00',
'2026-06-10 00:00:00'),

    ('MISSED_PAYMENT_EMAIL', 'EMAIL', 'URGENT: Overdue Repayment Notice',
    '<!DOCTYPE html><html><body style="font-family: Arial, sans-serif; color: #333333; line-height: 1.6;"><div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;"><h2 style="color: #c62828;">Repayment Notice Overdue</h2><p>Hello {{recipientName}},</p><p>Our system ledgers indicate that you have missed your scheduled repayment obligation window for loan profile reference <strong>{{accountReference}}</strong>.</p><div style="background-color: #ffebee; color: #c62828; padding: 15px; border-left: 4px solid #c62828; margin: 20px 0;"><strong>Overdue Balance Due:</strong> {{currency}} {{amount}}<br><strong>Original Due Date:</strong> {{dueDate}}</div><p>Please log in immediately to top up your mobile wallet or process a manual pay-in via M-Pesa to prevent penalty accumulation or credit rating degradation flags.</p></div></body></html>',
    '2026-06-10 00:00:00',
    '2026-06-10 00:00:00'),

    ('MISSED_PAYMENT_SMS', 'SMS', 'Overdue Alert',
    'Alert {{recipientName}}: Your repayment of KES {{amount}} for loan {{accountReference}} is overdue. Please pay immediately to prevent penalties.',
    '2026-06-10 00:00:00',
    '2026-06-10 00:00:00');
