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
