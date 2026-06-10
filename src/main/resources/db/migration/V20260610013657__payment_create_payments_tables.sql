CREATE TABLE payment_categories (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_channels (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_providers (
    id VARCHAR(50) PRIMARY KEY,
    channel_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active VARCHAR(5) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (channel_id) REFERENCES payment_channels(id)
);

CREATE TABLE payment_transaction_statuses (
    id VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_parties (
    id VARCHAR(50) PRIMARY KEY,
    party_reference VARCHAR(100) NOT NULL,
    display_name VARCHAR(150),
    party_type VARCHAR(30) NOT NULL,
    source_module VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_parties_reference
    ON payment_parties(party_reference);

CREATE TABLE payment_transactions (
    id VARCHAR(50) PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    category_id VARCHAR(50) NOT NULL,
    provider_id VARCHAR(50) NOT NULL,
    status_id VARCHAR(50) NOT NULL,
    account_reference VARCHAR(100) NOT NULL,
    loan_account_id VARCHAR(50),
    profile_id VARCHAR(64),
    sender_party_id VARCHAR(50) NOT NULL,
    receiver_party_id VARCHAR(50) NOT NULL,
    amount DECIMAL(18,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    initiated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,

    FOREIGN KEY (category_id) REFERENCES payment_categories(id),
    FOREIGN KEY (provider_id) REFERENCES payment_providers(id),
    FOREIGN KEY (status_id) REFERENCES payment_transaction_statuses(id),
    FOREIGN KEY (sender_party_id) REFERENCES payment_parties(id),
    FOREIGN KEY (receiver_party_id) REFERENCES payment_parties(id)
);

CREATE UNIQUE INDEX idx_tx_idempotency
    ON payment_transactions(idempotency_key);

CREATE INDEX idx_tx_account_ref
    ON payment_transactions(account_reference);

CREATE INDEX idx_tx_profile_id
    ON payment_transactions(profile_id);

CREATE TABLE payment_provider_metadata (
    id VARCHAR(50) PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(100),
    external_reference_number VARCHAR(100),
    raw_payload_dump TEXT,
    error_code VARCHAR(50),
    error_message TEXT,
    callback_received_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,

    FOREIGN KEY (transaction_id)
    REFERENCES payment_transactions(id)
);

CREATE INDEX idx_meta_tx_id
    ON payment_provider_metadata(transaction_id);

CREATE INDEX idx_meta_ext_ref
    ON payment_provider_metadata(external_reference_number);

CREATE INDEX idx_meta_provider_tx_id
    ON payment_provider_metadata(provider_transaction_id);

INSERT INTO payment_categories (id, name, description, created_at) VALUES
('DISBURSEMENT', 'Loan Disbursal', 'Outbound capital payout to a borrower account reference.', '2026-06-10 00:00:00'),
('REPAYMENT', 'Loan Repayment', 'Inbound installment or full principal/interest clearing pay-in.', '2026-06-10 00:00:00'),
('REVERSAL', 'Transaction Reversal', 'Corrective ledger entry reversing an error or failed settlement.', '2026-06-10 00:00:00'),
('CHARGE', 'Service Fee / Penalty', 'System or provider processing fees charged on transactions.', '2026-06-10 00:00:00');

INSERT INTO payment_channels (id, name, created_at) VALUES
('MOBILE_MONEY', 'Mobile Money Networks', '2026-06-10 00:00:00'),
('BANK_TRANSFER', 'Bank Processing Networks', '2026-06-10 00:00:00'),
('INTERNAL_WALLET', 'On-Platform Digital Ledger Wallets', '2026-06-10 00:00:00');

INSERT INTO payment_providers (id, channel_id, name, is_active, created_at) VALUES
('MPESA', 'MOBILE_MONEY', 'Safaricom M-Pesa', 'TRUE', '2026-06-10 00:00:00'),
('AIRTEL_MONEY', 'MOBILE_MONEY', 'Airtel Money', 'TRUE', '2026-06-10 00:00:00'),
('MTN_MOMO', 'MOBILE_MONEY', 'MTN Mobile Money', 'TRUE', '2026-06-10 00:00:00'),
('PESALINK', 'BANK_TRANSFER', 'PesaLink Realtime Transfer', 'TRUE', '2026-06-10 00:00:00'),
('EFT', 'BANK_TRANSFER', 'Electronic Funds Transfer (ACH/RTGS)', 'TRUE', '2026-06-10 00:00:00'),
('INTERNAL', 'INTERNAL_WALLET', 'Internal System Book-Ledger Payout', 'TRUE', '2026-06-10 00:00:00');

INSERT INTO payment_transaction_statuses (id, description, created_at) VALUES
('PENDING', 'Transaction initiated, awaiting external channel verification webhook or response.', '2026-06-10 00:00:00'),
('PROCESSING', 'Transaction received by upstream aggregator gateway and currently under clearance.', '2026-06-10 00:00:00'),
('COMPLETED', 'Funds successfully cleared and settled on the ledger cleanly.', '2026-06-10 00:00:00'),
('FAILED', 'Upstream gateway execution error or insufficient client coverage parameters.', '2026-06-10 00:00:00'),
('REVERSED', 'Funds returned completely back to original host sender balances.', '2026-06-10 00:00:00');
