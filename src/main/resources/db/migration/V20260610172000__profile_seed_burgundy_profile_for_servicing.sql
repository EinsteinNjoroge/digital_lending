INSERT INTO profile_profile (
    id,
    profile_type,
    email,
    phone_country_code,
    phone_national_number,
    residence_country,
    status,
    created_at,
    updated_at
) VALUES (
    'prof_burgundy_001',
    'INDIVIDUAL',
    'burgundy.evolution@gmail.com',
    '+254',
    '711223344',
    'KEN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO profile_individual_profile (
    id,
    first_name,
    last_name,
    date_of_birth
) VALUES (
    'prof_burgundy_001',
    'Burgundy',
    'Evolution',
    DATE '1991-04-17'
);

INSERT INTO profile_individual_identities (
    profile_id,
    document_type,
    document_number
) VALUES (
    'prof_burgundy_001',
    'NATIONAL_ID',
    'BURGUNDY-SEED-ID-001'
);
