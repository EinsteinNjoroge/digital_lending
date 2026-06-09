CREATE TABLE loan_products_family_definition (
	id VARCHAR(64) PRIMARY KEY,
	family_code VARCHAR(32) NOT NULL,
	display_name VARCHAR(100) NOT NULL,
	disbursement_handler_token VARCHAR(64) NOT NULL,
	accrual_handler_token VARCHAR(64) NOT NULL,
	repayment_handler_token VARCHAR(64) NOT NULL,
	delinquency_handler_token VARCHAR(64) NOT NULL,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT uq_lp_family_code UNIQUE (family_code)
);

CREATE TABLE loan_products_product_configuration (
	id VARCHAR(64) PRIMARY KEY,
	version INT NOT NULL,
	product_code VARCHAR(32) NOT NULL,
	name VARCHAR(100) NOT NULL,
	family_definition_id VARCHAR(64) NOT NULL,
	partner_id VARCHAR(64) NOT NULL,
	currency VARCHAR(3) NOT NULL,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT uq_lp_prod_code_version_v3 UNIQUE (product_code, version),
	CONSTRAINT fk_lp_pc_family_def FOREIGN KEY (family_definition_id) REFERENCES loan_products_family_definition(id)
);

CREATE TABLE loan_products_product_parameter (
	id BIGSERIAL PRIMARY KEY,
	product_id VARCHAR(64) NOT NULL,
	parameter_key VARCHAR(64) NOT NULL,
	parameter_value TEXT NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_lp_param_product FOREIGN KEY (product_id) REFERENCES loan_products_product_configuration(id) ON DELETE CASCADE,
	CONSTRAINT uq_lp_product_param_key UNIQUE (product_id, parameter_key)
);

CREATE TABLE loan_products_product_document_matrix (
	id BIGSERIAL PRIMARY KEY,
	product_id VARCHAR(64) NOT NULL,
	matrix_type VARCHAR(64) NOT NULL,
	payload JSONB NOT NULL DEFAULT '{}'::jsonb,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_lp_matrix_product FOREIGN KEY (product_id) REFERENCES loan_products_product_configuration(id) ON DELETE CASCADE
);

CREATE TABLE loan_products_product_configuration_audit_log (
	id BIGSERIAL PRIMARY KEY,
	product_id VARCHAR(64) NOT NULL,
	action_type VARCHAR(16) NOT NULL,
	modified_by VARCHAR(100) NOT NULL,
	changed_attributes JSONB,
	timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lp_abstract_product_routing ON loan_products_product_configuration (partner_id, currency, is_active);
CREATE INDEX idx_lp_param_lookup_kv ON loan_products_product_parameter (product_id, parameter_key);
CREATE INDEX idx_lp_matrix_jsonb_path ON loan_products_product_document_matrix USING gin (payload);

INSERT INTO loan_products_family_definition (id, family_code, display_name, disbursement_handler_token, accrual_handler_token, repayment_handler_token, delinquency_handler_token) VALUES
(
	'fdef_001', 'FAM_AIRTIME_CREDIT', 'Airtime Advance Services',
	'DISB_IMMEDIATE_TELCO_BAL', 'ACCR_FIXED_ONE_TIME_FEE', 'REPAY_AUTO_AIRTIME_RECHARGE', 'DELINQ_BLOCK_FUTURE_ADVANCE'
),
(
	'fdef_002', 'FAM_MOBILE_WALLET_NANO', 'Mobile Wallet Cash Injection',
	'DISB_IMMEDIATE_WALLET_PUSH', 'ACCR_SIMPLE_DAILY_LOOP', 'REPAY_MOBILE_MONEY_DEBIT', 'DELINQ_PENALTY_ACCUMULATION'
),
(
	'fdef_003', 'FAM_LINE_OF_CREDIT', 'Revolving Line of Credit',
	'DISB_ON_DEMAND_DRAWDOWN', 'ACCR_DECLINING_BALANCE_DAILY', 'REPAY_MIN_DUE_AUTO_SWEEP', 'DELINQ_SUSPEND_CREDIT_FACILITY'
),
(
	'fdef_004', 'FAM_BNPL', 'Buy Now Pay Later Asset Plan',
	'DISB_MERCHANT_MILESTONE_SETTLE', 'ACCR_AMORTIZED_EQUAL_INSTALLMENTS', 'REPAY_SCHEDULED_ESCROW_PULL', 'DELINQ_DEVICE_REMOTE_LOCKOUT'
);