CREATE TABLE profile_profile (
    id VARCHAR(50) NOT NULL,
    profile_type VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_country_code VARCHAR(5) NOT NULL,
    phone_national_number VARCHAR(15) NOT NULL,
    residence_country VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_profile_profile PRIMARY KEY (id),
    CONSTRAINT uq_profile_profile_email UNIQUE (email)
);

CREATE TABLE profile_individual_profile (
    id VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    CONSTRAINT pk_profile_individual PRIMARY KEY (id),
    CONSTRAINT fk_profile_individual_base FOREIGN KEY (id) REFERENCES profile_profile (id) ON DELETE CASCADE
);

CREATE TABLE profile_corporate_profile (
    id VARCHAR(50) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    registration_number VARCHAR(50) NOT NULL,
    incorporation_date DATE NOT NULL,
    authorized_signatory_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_profile_corporate PRIMARY KEY (id),
    CONSTRAINT fk_profile_corporate_base FOREIGN KEY (id) REFERENCES profile_profile (id) ON DELETE CASCADE,
    CONSTRAINT uq_profile_corporate_reg_num UNIQUE (registration_number)
);

CREATE TABLE profile_joint_profile (
    id VARCHAR(50) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    primary_contact_name VARCHAR(100) NOT NULL,
    number_of_applicants INT NOT NULL,
    CONSTRAINT pk_profile_joint PRIMARY KEY (id),
    CONSTRAINT fk_profile_joint_base FOREIGN KEY (id) REFERENCES profile_profile (id) ON DELETE CASCADE
);

CREATE TABLE profile_individual_identities (
    profile_id VARCHAR(50) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    CONSTRAINT fk_profile_individual_identities_base FOREIGN KEY (profile_id) REFERENCES profile_individual_profile(id) ON DELETE CASCADE
);

CREATE TABLE profile_corporate_director_identities (
    profile_id VARCHAR(50) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    CONSTRAINT fk_profile_corporate_identities_base FOREIGN KEY (profile_id) REFERENCES profile_corporate_profile(id) ON DELETE CASCADE
);

CREATE TABLE profile_joint_applicant_identities (
    profile_id VARCHAR(50) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    CONSTRAINT fk_profile_joint_identities_base FOREIGN KEY (profile_id) REFERENCES profile_joint_profile(id) ON DELETE CASCADE
);

CREATE INDEX idx_profile_base_status ON profile_profile(status);
CREATE INDEX idx_profile_base_email ON profile_profile(email);
CREATE INDEX idx_profile_individual_ident_lookup ON profile_individual_identities(document_type, document_number);
CREATE INDEX idx_profile_corporate_ident_lookup ON profile_corporate_director_identities(document_type, document_number);
CREATE INDEX idx_profile_joint_ident_lookup ON profile_joint_applicant_identities(document_type, document_number);
