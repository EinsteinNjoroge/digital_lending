UPDATE notification_templates
SET title_template = 'Overdue repayment notice - {{performanceStatus}}',
    body_template = '<!DOCTYPE html><html><body style="font-family: Arial, sans-serif; color: #333333; line-height: 1.6;"><div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;"><h2 style="color: #c62828;">Repayment notice for {{recipientName}}</h2><p>Your loan <strong>{{accountReference}}</strong> is now in <strong>{{performanceStatus}}</strong> status.</p><div style="background-color: #ffebee; color: #c62828; padding: 15px; border-left: 4px solid #c62828; margin: 20px 0;"><strong>Outstanding Balance:</strong> {{currency}} {{amount}}<br><strong>Original Due Date:</strong> {{dueDate}}<br><strong>Days Past Due:</strong> {{daysPastDue}}</div><p>Please make a repayment as soon as possible to avoid further collections action.</p></div></body></html>',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'MISSED_PAYMENT_EMAIL';

UPDATE notification_templates
SET title_template = 'Overdue repayment - {{performanceStatus}}',
    body_template = 'Alert {{recipientName}}: loan {{accountReference}} is in {{performanceStatus}} status with {{daysPastDue}} days past due. Outstanding balance: {{currency}} {{amount}}.',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'MISSED_PAYMENT_SMS';
