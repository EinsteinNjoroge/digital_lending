ALTER TABLE loan_account_accounts
    ADD COLUMN repayment_due_at TIMESTAMPTZ,
    ADD COLUMN days_past_due INT NOT NULL DEFAULT 0,
    ADD COLUMN last_serviced_at TIMESTAMPTZ;

CREATE INDEX idx_loan_account_repayment_due_at
    ON loan_account_accounts (repayment_due_at);
