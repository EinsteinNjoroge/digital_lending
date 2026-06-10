

CREATE TABLE loan_account_product_configuration_projection (
    loan_product_id VARCHAR(64) NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    partner_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL,
    repayment_due_days BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_loan_account_product_projection PRIMARY KEY (loan_product_id)
);

INSERT INTO loan_account_product_configuration_projection (
    loan_product_id,
    product_code,
    partner_id,
    currency,
    is_active,
    repayment_due_days,
    updated_at
)
SELECT
    pc.id,
    pc.product_code,
    pc.partner_id,
    pc.currency,
    pc.is_active,
    COALESCE(
        MAX(CASE WHEN lower(pp.parameter_key) = 'repayment_due_days' AND pp.parameter_value ~ '^[0-9]+$' THEN pp.parameter_value::BIGINT END),
        MAX(CASE WHEN lower(pp.parameter_key) = 'max_tenor_days' AND pp.parameter_value ~ '^[0-9]+$' THEN pp.parameter_value::BIGINT END),
        MAX(CASE WHEN lower(pp.parameter_key) = 'review_cycle_days' AND pp.parameter_value ~ '^[0-9]+$' THEN pp.parameter_value::BIGINT END),
        MAX(CASE WHEN lower(pp.parameter_key) = 'season_length_days' AND pp.parameter_value ~ '^[0-9]+$' THEN pp.parameter_value::BIGINT END),
        MAX(CASE WHEN lower(pp.parameter_key) = 'merchant_settlement_delay_days' AND pp.parameter_value ~ '^[0-9]+$' THEN pp.parameter_value::BIGINT END),
        30
    ) AS repayment_due_days,
    pc.updated_at
FROM loan_products_product_configuration pc
LEFT JOIN loan_products_product_parameter pp ON pp.product_id = pc.id
GROUP BY pc.id, pc.product_code, pc.partner_id, pc.currency, pc.is_active, pc.updated_at;

CREATE TABLE notification_profile_contact_projection (
    profile_id VARCHAR(50) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_notification_profile_contact_projection PRIMARY KEY (profile_id)
);

INSERT INTO notification_profile_contact_projection (
    profile_id,
    display_name,
    email,
    phone,
    status,
    updated_at
)
SELECT
    p.id,
    COALESCE(
        CASE WHEN p.profile_type = 'INDIVIDUAL' THEN trim(concat(i.first_name, ' ', i.last_name)) END,
        CASE WHEN p.profile_type = 'CORPORATE' THEN concat(c.company_name, ' (LLC)') END,
        CASE WHEN p.profile_type = 'JOINT' THEN concat(j.account_name, ' (Joint Account)') END,
        p.email,
        p.id
    ) AS display_name,
    p.email,
    concat(p.phone_country_code, p.phone_national_number) AS phone,
    p.status,
    p.updated_at
FROM profile_profile p
LEFT JOIN profile_individual_profile i ON i.id = p.id
LEFT JOIN profile_corporate_profile c ON c.id = p.id
LEFT JOIN profile_joint_profile j ON j.id = p.id;

CREATE TABLE creditscoring_loan_account_exposure_projection (
    loan_account_id VARCHAR(50) NOT NULL,
    profile_id VARCHAR(50) NOT NULL,
    account_reference VARCHAR(32),
    outstanding_principal NUMERIC(18, 4) NOT NULL,
    exposure_status VARCHAR(32) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_creditscoring_loan_account_exposure_projection PRIMARY KEY (loan_account_id)
);

CREATE INDEX idx_creditscoring_loan_account_exposure_profile_status
    ON creditscoring_loan_account_exposure_projection (profile_id, exposure_status);

INSERT INTO creditscoring_loan_account_exposure_projection (
    loan_account_id,
    profile_id,
    account_reference,
    outstanding_principal,
    exposure_status,
    updated_at
)
SELECT
    id,
    profile_id,
    account_number,
    outstanding_principal,
    issuance_status,
    updated_at
FROM loan_account_accounts;
