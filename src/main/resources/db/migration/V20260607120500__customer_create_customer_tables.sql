CREATE TABLE customer_customer (
    id VARCHAR(50) NOT NULL,
    customer_type VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_country_code VARCHAR(5) NOT NULL,       --"+254", "+1"
    phone_national_number VARCHAR(15) NOT NULL,   -- "712345678"
    residence_country VARCHAR(3) NOT NULL,         -- "KEN", "USA")
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_customer_customer PRIMARY KEY (id),
    CONSTRAINT uq_customer_customer_email UNIQUE (email)
);

CREATE TABLE customer_individual_customer (
    id VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    CONSTRAINT pk_customer_individual PRIMARY KEY (id),
    CONSTRAINT fk_customer_individual_base FOREIGN KEY (id) REFERENCES customer_customer (id) ON DELETE CASCADE
);

CREATE TABLE customer_corporate_customer (
    id VARCHAR(50) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    registration_number VARCHAR(50) NOT NULL,
    incorporation_date DATE NOT NULL,
    authorized_signatory_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_customer_corporate PRIMARY KEY (id),
    CONSTRAINT fk_customer_corporate_base FOREIGN KEY (id) REFERENCES customer_customer (id) ON DELETE CASCADE,
    CONSTRAINT uq_customer_corporate_reg_num UNIQUE (registration_number)
);

CREATE TABLE customer_joint_customer (
    id VARCHAR(50) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    primary_contact_name VARCHAR(100) NOT NULL,
    number_of_applicants INT NOT NULL,
    CONSTRAINT pk_customer_joint PRIMARY KEY (id),
    CONSTRAINT fk_customer_joint_base FOREIGN KEY (id) REFERENCES customer_customer (id) ON DELETE CASCADE
);

CREATE TABLE customer_individual_identities (
    customer_id VARCHAR(50) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    CONSTRAINT fk_individual_identities_base FOREIGN KEY (customer_id) REFERENCES customer_individual_customer(id) ON DELETE CASCADE
);

CREATE TABLE customer_corporate_director_identities (
    customer_id VARCHAR(50) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    CONSTRAINT fk_corporate_identities_base FOREIGN KEY (customer_id) REFERENCES customer_corporate_customer(id) ON DELETE CASCADE
);

CREATE TABLE customer_joint_applicant_identities (
    customer_id VARCHAR(50) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    CONSTRAINT fk_joint_identities_base FOREIGN KEY (customer_id) REFERENCES customer_joint_customer(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_base_status ON customer_customer(status);
CREATE INDEX idx_customer_base_email ON customer_customer(email);
CREATE INDEX idx_individual_ident_lookup ON customer_individual_identities(document_type, document_number);
CREATE INDEX idx_corporate_ident_lookup ON customer_corporate_director_identities(document_type, document_number);
CREATE INDEX idx_joint_ident_lookup ON customer_joint_applicant_identities(document_type, document_number);