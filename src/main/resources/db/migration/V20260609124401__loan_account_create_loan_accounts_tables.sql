-- Re-architected schema supporting multi-state tracking and audit capabilities
CREATE TABLE loan_account_accounts (
    id VARCHAR(50) NOT NULL,
    account_number VARCHAR(32), -- Nullable initially while in DRAFT status
    profile_id VARCHAR(50) NOT NULL,
    loan_product_id VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    initial_principal NUMERIC(18, 4) NOT NULL,
    credit_limit_at_capture INT,
    issuance_status VARCHAR(20) NOT NULL, -- DRAFT, APPROVED_ISSUED, DENIED
    performance_status VARCHAR(16),       -- ACTIVE, WATCH, DOUBTFUL, SETTLED (Null if Denied)
    parent_loan_account_id VARCHAR(50),
    taken_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loan_account_accounts PRIMARY KEY (id),
    CONSTRAINT uk_loan_account_accounts_account_number UNIQUE (account_number),
    CONSTRAINT uk_loan_account_accounts_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_loan_account_accounts_parent FOREIGN KEY (parent_loan_account_id) REFERENCES loan_account_accounts(id)
);

-- Partial Unique Index protecting credit lines: Prevents concurrent active exposures per product
CREATE UNIQUE INDEX uk_active_product_per_profile 
ON loan_account_accounts (profile_id, loan_product_id) 
WHERE issuance_status = 'APPROVED_ISSUED' AND performance_status IN ('ACTIVE', 'WATCH', 'DOUBTFUL');

-- Append-Only Structural Audit Log Table
CREATE TABLE loan_account_audit_logs (
    id VARCHAR(50) NOT NULL,
    loan_account_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- DRAWDOWN_INITIATED, CREDIT_CHECK_PASSED, CREDIT_CHECK_FAILED, PERFORMANCE_STATUS_CHANGED
    previous_state TEXT,
    new_state TEXT NOT NULL,
    credit_score_decision_id VARCHAR(100), -- Captured from credit scoring service engine
    modified_by VARCHAR(100) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loan_account_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_loan_account FOREIGN KEY (loan_account_id) REFERENCES loan_account_accounts(id)
);

CREATE INDEX idx_loan_audit_account ON loan_account_audit_logs (loan_account_id);