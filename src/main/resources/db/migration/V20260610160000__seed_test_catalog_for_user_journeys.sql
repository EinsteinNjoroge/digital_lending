INSERT INTO loan_products_family_definition (
    id,
    family_code,
    display_name,
    disbursement_handler_token,
    accrual_handler_token,
    repayment_handler_token,
    delinquency_handler_token,
    is_active,
    created_at,
    updated_at
) VALUES
    ('fdef_005', 'FAM_SALARY_ADVANCE', 'Salary Advance Loans', 'DISB_EMPLOYER_WALLET_PUSH', 'ACCR_SIMPLE_DAILY_LOOP', 'REPAY_PAYROLL_DEDUCTION', 'DELINQ_EMPLOYER_RECOVERY_FOLLOWUP', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('fdef_006', 'FAM_DEVICE_FINANCE', 'Device Financing', 'DISB_MERCHANT_MILESTONE_SETTLE', 'ACCR_AMORTIZED_EQUAL_INSTALLMENTS', 'REPAY_SCHEDULED_ESCROW_PULL', 'DELINQ_DEVICE_REMOTE_LOCKOUT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('fdef_007', 'FAM_SME_WORKING_CAP', 'SME Working Capital', 'DISB_BANK_TRANSFER_BATCH', 'ACCR_DECLINING_BALANCE_DAILY', 'REPAY_BUSINESS_COLLECTION_SWEEP', 'DELINQ_FIELD_COLLECTION_ESCALATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('fdef_008', 'FAM_AGRI_INPUT_FIN', 'Agri Input Finance', 'DISB_SUPPLIER_SETTLEMENT', 'ACCR_SEASONAL_INTEREST', 'REPAY_HARVEST_PROCEEDS_SWEEP', 'DELINQ_GROUP_GUARANTEE_ESCALATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('fdef_009', 'FAM_EDU_BRIDGE', 'Education Fee Bridge Loans', 'DISB_SCHOOL_ESCROW_SETTLE', 'ACCR_SIMPLE_MONTHLY_FLAT', 'REPAY_SCHEDULED_MOBILE_MONEY', 'DELINQ_GUARDIAN_ESCALATION_TRACK', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('fdef_010', 'FAM_MERCHANT_CASH_ADV', 'Merchant Cash Advance', 'DISB_BUSINESS_FLOAT_TOPUP', 'ACCR_REVENUE_SHARE', 'REPAY_DAILY_SETTLEMENT_SPLIT', 'DELINQ_SETTLEMENT_HOLDBACK_INCREASE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO loan_products_product_configuration (
    id,
    version,
    product_code,
    name,
    family_definition_id,
    partner_id,
    currency,
    is_active,
    created_at,
    updated_at
) VALUES
    ('prod_001', 1, 'LP_SAF_KE_NANO', 'Safaricom Kenya Nano Cash', 'fdef_002', 'SAF_KE_01', 'KES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_002', 1, 'LP_SAF_KE_FLEXI', 'Safaricom Kenya Flexi Wallet Loan', 'fdef_002', 'SAF_KE_01', 'KES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_003', 1, 'LP_SAF_KE_SALARY', 'Safaricom Kenya Salary Advance', 'fdef_005', 'SAF_KE_01', 'KES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_004', 1, 'LP_AIR_UG_NANO', 'Airtel Uganda Nano Loan', 'fdef_002', 'AIRTEL_UG_01', 'UGX', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_005', 1, 'LP_AIR_UG_SALARY', 'Airtel Uganda Salary Advance', 'fdef_005', 'AIRTEL_UG_01', 'UGX', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_006', 1, 'LP_MTN_GH_BNPL', 'MTN Ghana BNPL', 'fdef_004', 'MTN_GH_01', 'GHS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_007', 1, 'LP_VODA_TZ_DEVICE', 'Vodacom Tanzania Device Finance', 'fdef_006', 'VODA_TZ_01', 'TZS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_008', 1, 'LP_EQ_KE_SME', 'Equity Kenya SME Working Capital', 'fdef_007', 'EQ_BANK_KE_01', 'KES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_009', 1, 'LP_EQ_KE_LOC', 'Equity Kenya Line of Credit', 'fdef_003', 'EQ_BANK_KE_01', 'KES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_010', 1, 'LP_AGRI_KE_INPUT', 'Kenya Agri Input Bridge', 'fdef_008', 'AGRI_COOP_KE_01', 'KES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_011', 1, 'LP_SCH_KE_BRIDGE', 'Kenya Education Fee Bridge', 'fdef_009', 'EDU_FIN_KE_01', 'KES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_012', 1, 'LP_MERCH_KE_ADV', 'Kenya Merchant Cash Advance', 'fdef_010', 'MERCH_PAY_KE_01', 'KES', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO loan_products_product_parameter (product_id, parameter_key, parameter_value, created_at, updated_at) VALUES
    ('prod_001', 'interest_rate_daily', '0.0015', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_001', 'max_tenor_days', '30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_001', 'minimum_principal', '500', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_001', 'maximum_principal', '8000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_002', 'interest_rate_daily', '0.0012', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_002', 'max_tenor_days', '45', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_002', 'minimum_principal', '1000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_002', 'maximum_principal', '15000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_003', 'interest_rate_daily', '0.0008', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_003', 'max_tenor_days', '60', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_003', 'minimum_principal', '2000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_003', 'maximum_principal', '30000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_004', 'interest_rate_daily', '0.0018', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_004', 'max_tenor_days', '21', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_004', 'minimum_principal', '10000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_004', 'maximum_principal', '500000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_005', 'interest_rate_daily', '0.0010', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_005', 'max_tenor_days', '60', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_005', 'minimum_principal', '50000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_005', 'maximum_principal', '1500000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_006', 'installment_count', '4', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_006', 'minimum_principal', '300', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_006', 'maximum_principal', '5000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_006', 'merchant_settlement_delay_days', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_007', 'installment_count', '12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_007', 'minimum_principal', '50000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_007', 'maximum_principal', '3000000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_007', 'asset_ltv_cap', '0.85', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_008', 'interest_rate_daily', '0.0009', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_008', 'max_tenor_days', '120', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_008', 'minimum_principal', '25000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_008', 'maximum_principal', '2500000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_009', 'interest_rate_daily', '0.0007', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_009', 'review_cycle_days', '30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_009', 'minimum_principal', '10000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_009', 'maximum_principal', '500000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_010', 'interest_rate_monthly', '0.035', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_010', 'season_length_days', '180', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_010', 'minimum_principal', '5000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_010', 'maximum_principal', '500000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_011', 'interest_rate_monthly', '0.025', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_011', 'max_tenor_days', '90', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_011', 'minimum_principal', '2000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_011', 'maximum_principal', '200000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('prod_012', 'holdback_percentage', '0.15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_012', 'minimum_principal', '10000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_012', 'maximum_principal', '1000000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_012', 'settlement_frequency_days', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO loan_products_product_document_matrix (product_id, matrix_type, payload, created_at, updated_at) VALUES
    ('prod_001', 'UNDERWRITING_CHECKLIST', '{"minimum_wallet_age_months": 3, "require_identity_verification": true, "allow_repeat_borrowers_only": false}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_002', 'UNDERWRITING_CHECKLIST', '{"minimum_wallet_age_months": 6, "require_identity_verification": true, "require_income_proxy": true}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_003', 'UNDERWRITING_CHECKLIST', '{"minimum_employment_months": 3, "require_employer_code": true, "allow_existing_salary_customers_only": true}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_004', 'UNDERWRITING_CHECKLIST', '{"minimum_wallet_age_months": 2, "require_identity_verification": true, "max_active_loans": 1}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_005', 'UNDERWRITING_CHECKLIST', '{"minimum_employment_months": 6, "require_employer_code": true, "require_payroll_consistency": true}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_006', 'MERCHANT_RULES', '{"merchant_onboarding_required": true, "down_payment_percentage": 0.10, "max_device_value": 5000}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_007', 'ASSET_RULES', '{"imei_capture_required": true, "device_insurance_required": true, "dealer_whitelist_only": true}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_008', 'BUSINESS_RULES', '{"minimum_business_age_months": 12, "require_statement_analysis": true, "sector_blacklist": ["GAMBLING"]}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_009', 'FACILITY_RULES', '{"allow_multiple_drawdowns": true, "minimum_review_score": 250, "max_utilization_ratio": 0.90}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_010', 'AGRI_RULES', '{"crop_cycle_required": true, "group_guarantee_supported": true, "seasonal_disbursement_only": true}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_011', 'EDUCATION_RULES', '{"school_invoice_required": true, "guardian_contact_required": true, "max_dependants_supported": 4}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('prod_012', 'MERCHANT_RULES', '{"minimum_settlement_history_days": 90, "merchant_kibana_score_required": true, "daily_split_percentage_cap": 0.20}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO credit_scoring_model_definition (
    id,
    loan_product_id,
    partner_id,
    currency,
    is_active,
    rules_payload,
    created_at,
    updated_at
) VALUES
    (
        'csm_001',
        'prod_001',
        'SAF_KE_01',
        'KES',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 9999.99, "points": 150},
              {"min": 10000, "max": 19999.99, "points": 250},
              {"min": 20000, "max": 999999999, "points": 400}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 200, "baseMultiplier": 0.50}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_002',
        'prod_002',
        'SAF_KE_01',
        'KES',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"},
            {"feature": "fraud_watchlist_hit", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 14999.99, "points": 120},
              {"min": 15000, "max": 49999.99, "points": 280},
              {"min": 50000, "max": 999999999, "points": 420}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 240, "baseMultiplier": 0.60}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_003',
        'prod_003',
        'SAF_KE_01',
        'KES',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 29999.99, "points": 180},
              {"min": 30000, "max": 79999.99, "points": 320},
              {"min": 80000, "max": 999999999, "points": 480}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 220, "baseMultiplier": 0.80}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_004',
        'prod_004',
        'AIRTEL_UG_01',
        'UGX',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 199999.99, "points": 140},
              {"min": 200000, "max": 699999.99, "points": 260},
              {"min": 700000, "max": 999999999, "points": 390}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 180, "baseMultiplier": 0.45}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_005',
        'prod_005',
        'AIRTEL_UG_01',
        'UGX',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"},
            {"feature": "payroll_disruption_flag", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 499999.99, "points": 160},
              {"min": 500000, "max": 1499999.99, "points": 300},
              {"min": 1500000, "max": 999999999, "points": 460}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 210, "baseMultiplier": 0.70}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_006',
        'prod_006',
        'MTN_GH_01',
        'GHS',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"},
            {"feature": "merchant_blacklist_hit", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 999.99, "points": 130},
              {"min": 1000, "max": 2999.99, "points": 240},
              {"min": 3000, "max": 999999999, "points": 360}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 170, "baseMultiplier": 0.40}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_007',
        'prod_007',
        'VODA_TZ_01',
        'TZS',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"},
            {"feature": "device_blacklist_hit", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 499999.99, "points": 150},
              {"min": 500000, "max": 1999999.99, "points": 280},
              {"min": 2000000, "max": 999999999, "points": 430}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 230, "baseMultiplier": 0.75}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_008',
        'prod_008',
        'EQ_BANK_KE_01',
        'KES',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"},
            {"feature": "business_registry_inactive", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 99999.99, "points": 180},
              {"min": 100000, "max": 499999.99, "points": 320},
              {"min": 500000, "max": 999999999, "points": 500}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 260, "baseMultiplier": 0.85}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_009',
        'prod_009',
        'EQ_BANK_KE_01',
        'KES',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 49999.99, "points": 170},
              {"min": 50000, "max": 199999.99, "points": 290},
              {"min": 200000, "max": 999999999, "points": 430}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 230, "baseMultiplier": 0.65}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_010',
        'prod_010',
        'AGRI_COOP_KE_01',
        'KES',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"},
            {"feature": "crop_failure_alert", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 19999.99, "points": 140},
              {"min": 20000, "max": 99999.99, "points": 260},
              {"min": 100000, "max": 999999999, "points": 390}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 190, "baseMultiplier": 0.55}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_011',
        'prod_011',
        'EDU_FIN_KE_01',
        'KES',
        TRUE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 14999.99, "points": 150},
              {"min": 15000, "max": 59999.99, "points": 270},
              {"min": 60000, "max": 999999999, "points": 410}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 200, "baseMultiplier": 0.60}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'csm_012',
        'prod_012',
        'MERCH_PAY_KE_01',
        'KES',
        FALSE,
        '{
          "knockOutRules": [
            {"feature": "has_active_default", "operator": "EQUALS", "value": "true"}
          ],
          "scoringWeights": [
            {"feature": "wallet_throughput_30d", "ranges": [
              {"min": 0, "max": 39999.99, "points": 120},
              {"min": 40000, "max": 149999.99, "points": 230},
              {"min": 150000, "max": 999999999, "points": 340}
            ]}
          ],
          "decisionThresholds": {"minimumScoreRequired": 210, "baseMultiplier": 0.50}
        }'::jsonb,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
