-- V4__add_model_provider.sql
-- Add model_provider table and link to provider table
USE prismnetai;

-- Create model_provider table
CREATE TABLE model_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


-- Add model_provider_id to provider table
ALTER TABLE provider ADD COLUMN model_provider_id BIGINT,
ADD CONSTRAINT fk_provider_model_provider FOREIGN KEY (model_provider_id) REFERENCES model_provider(id);

-- Insert initial model_provider records
INSERT INTO model_provider (name, email, phone, address, is_active) VALUES
('OpenAI Inc.', 'contact@openai.com', '+1-555-0100', 'San Francisco, CA, USA', TRUE),
('Anthropic PBC', 'contact@anthropic.com', '+1-555-0200', 'San Francisco, CA, USA', TRUE),
('Google LLC', 'contact@google.com', '+1-555-0300', 'Mountain View, CA, USA', TRUE),
('Cohere Inc.', 'contact@cohere.com', '+1-555-0400', 'Toronto, ON, Canada', TRUE),
('Hugging Face Inc.', 'contact@huggingface.co', '+33-1-55-05-00', 'Paris, France', FALSE);

-- Update existing provider records with model_provider_id
UPDATE provider SET model_provider_id = 1 WHERE name = 'OpenAI';
UPDATE provider SET model_provider_id = 2 WHERE name = 'Anthropic';
UPDATE provider SET model_provider_id = 3 WHERE name = 'Google';
UPDATE provider SET model_provider_id = 4 WHERE name = 'Cohere';
UPDATE provider SET model_provider_id = 5 WHERE name = 'HuggingFace';