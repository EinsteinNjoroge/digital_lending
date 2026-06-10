ALTER TABLE credit_scoring_model_definition
    RENAME COLUMN model_code TO loan_product_id;

ALTER TABLE credit_scoring_model_definition
    ALTER COLUMN loan_product_id TYPE VARCHAR(64);
