-- V1__initial_schema.sql
-- Initial database schema for PrismNet AI Provider Routing
USE prismnetai;

-- Create providers table
CREATE TABLE provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    base_url VARCHAR(500) NOT NULL,
    api_key VARCHAR(500) NOT NULL, -- Encrypted in application
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_provider_active ON provider (is_active);
CREATE INDEX idx_provider_name ON provider (name);

-- Create models table
CREATE TABLE model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    context_window INT NOT NULL,
    input_pricing DECIMAL(10,6) NOT NULL,
    output_pricing DECIMAL(10,6) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (provider_id) REFERENCES provider(id)
);

CREATE UNIQUE INDEX uk_model_provider_model ON model (provider_id, model_id);
CREATE INDEX idx_model_active ON model (is_active);
CREATE INDEX idx_model_provider ON model (provider_id);

-- Create ai_requests table
CREATE TABLE ai_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    routing_strategy VARCHAR(20) NOT NULL DEFAULT 'PRICE',
    prompt TEXT NOT NULL,
    max_tokens INT,
    temperature DECIMAL(3,2),
    selected_provider_id BIGINT,
    selected_model_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    response TEXT,
    tokens_used INT,
    cost DECIMAL(10,6),
    latency_ms BIGINT,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    FOREIGN KEY (selected_provider_id) REFERENCES provider(id),
    FOREIGN KEY (selected_model_id) REFERENCES model(id)
);

CREATE INDEX idx_request_user_created ON ai_request (user_id, created_at);
CREATE INDEX idx_request_status_created ON ai_request (status, created_at);
CREATE INDEX idx_request_provider ON ai_request (selected_provider_id);
CREATE INDEX idx_request_strategy ON ai_request (routing_strategy);

-- Create routing_rules table
CREATE TABLE routing_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    provider_order VARCHAR(1000) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_rule_user_name ON routing_rule (user_id, name);
CREATE INDEX idx_rule_user_active ON routing_rule (user_id, is_active);
CREATE INDEX idx_rule_user_priority ON routing_rule (user_id, is_active, id);

-- Create provider_metrics table
CREATE TABLE provider_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    metric_type VARCHAR(20) NOT NULL,
    value DECIMAL(10,4) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (provider_id) REFERENCES provider(id)
);

CREATE INDEX idx_metric_provider_type_time ON provider_metric (provider_id, metric_type, timestamp);
CREATE INDEX idx_metric_timestamp ON provider_metric (timestamp);

-- Create model_metrics table
CREATE TABLE model_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    metric_type VARCHAR(20) NOT NULL,
    metric_value DECIMAL(5,4) NOT NULL, -- 0.0000 to 1.0000 for quality metrics
    sample_size INT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (model_id) REFERENCES model(id)
);

CREATE INDEX idx_model_metric_type_time ON model_metric (model_id, metric_type, timestamp);
CREATE INDEX idx_model_metric_timestamp ON model_metric (timestamp);
-- ALTER TABLE model_metric ADD CONSTRAINT chk_value_range CHECK (metric_value >= 0.0 AND metric_value <= 1.0);

-- Create routing_decisions table
CREATE TABLE routing_decision (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    strategy VARCHAR(20) NOT NULL,
    considered_providers VARCHAR(1000),
    selected_provider_id BIGINT NOT NULL,
    selection_reason VARCHAR(500) NOT NULL,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (request_id) REFERENCES ai_request(id),
    FOREIGN KEY (selected_provider_id) REFERENCES provider(id)
);

CREATE INDEX idx_decision_request ON routing_decision (request_id);
CREATE INDEX idx_decision_provider ON routing_decision (selected_provider_id);
CREATE INDEX idx_decision_created ON routing_decision (created_at);

-- Insert initial provider data
INSERT INTO provider (name, base_url, api_key, is_active) VALUES
('OpenAI', 'https://api.openai.com', 'placeholder-key', TRUE),
('Anthropic', 'https://api.anthropic.com', 'placeholder-key', TRUE);

-- Insert initial model data
INSERT INTO model (provider_id, model_id, name, context_window, input_pricing, output_pricing, is_active) VALUES
(1, 'gpt-4', 'GPT-4', 8192, 0.000030, 0.000060, TRUE),
(1, 'gpt-3.5-turbo', 'GPT-3.5 Turbo', 4096, 0.000002, 0.000002, TRUE),
(2, 'claude-3-opus', 'Claude 3 Opus', 200000, 0.000015, 0.000075, TRUE),
(2, 'claude-3-sonnet', 'Claude 3 Sonnet', 200000, 0.000003, 0.000015, TRUE);
