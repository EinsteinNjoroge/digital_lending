CREATE TABLE credit_scoring_model_definition (
    id VARCHAR(64) PRIMARY KEY,
    model_code VARCHAR(32) NOT NULL,
    partner_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rules_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_credit_scoring_model_partner_currency
    UNIQUE (model_code, partner_id, currency)
);

CREATE TABLE credit_profile (
    profile_id VARCHAR(64) PRIMARY KEY,
    baseline_score NUMERIC(10,2) NOT NULL,
    introductory_credit_limit NUMERIC(18,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credit_scoring_decision_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    profile_id VARCHAR(64) NOT NULL,
    partner_id VARCHAR(64) NOT NULL,
    model_definition_id VARCHAR(64) NOT NULL
    REFERENCES credit_scoring_model_definition(id),

    score_calculated NUMERIC(6,2) NOT NULL,
    decision_outcome VARCHAR(16) NOT NULL,
    credit_limit_allocated NUMERIC(18,4) NOT NULL DEFAULT 0.0000,

    feature_snapshot JSONB NOT NULL,
    evaluation_trace JSONB NOT NULL,

    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_credit_scoring_model_routing
    ON credit_scoring_model_definition (partner_id, currency, is_active);

CREATE INDEX idx_credit_scoring_decision_audit
    ON credit_scoring_decision_log (profile_id, evaluated_at DESC);
