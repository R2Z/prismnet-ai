-- V2__test_data.sql
-- Test data for comprehensive testing scenarios

USE prismnetai;

-- Insert additional providers for testing
INSERT INTO provider (name, base_url, api_key, is_active) VALUES
('Google', 'https://generativelanguage.googleapis.com', 'placeholder-key', TRUE),
('Cohere', 'https://api.cohere.ai', 'placeholder-key', TRUE),
('HuggingFace', 'https://api-inference.huggingface.co', 'placeholder-key', FALSE);

-- Insert additional models for testing
INSERT INTO model (provider_id, model_id, name, context_window, input_pricing, output_pricing, is_active) VALUES
-- Google models
(3, 'gemini-pro', 'Gemini Pro', 32768, 0.000001, 0.000004, TRUE),
(3, 'gemini-pro-vision', 'Gemini Pro Vision', 16384, 0.000004, 0.000012, TRUE),
-- Cohere models
(4, 'command', 'Command', 4096, 0.000015, 0.000075, TRUE),
(4, 'command-light', 'Command Light', 4096, 0.000008, 0.000038, TRUE),
-- HuggingFace models (inactive)
(5, 'gpt2', 'GPT-2', 1024, 0.000000, 0.000000, FALSE),
(5, 'distilbert', 'DistilBERT', 512, 0.000000, 0.000000, FALSE);

-- Insert test users' routing rules
INSERT INTO routing_rule (user_id, name, description, provider_order, is_active) VALUES
('test-user-1', 'Cost Optimized', 'Prioritize cheapest providers', '["OpenAI", "Google", "Cohere", "Anthropic"]', TRUE),
('test-user-1', 'Quality First', 'Prioritize best models', '["Anthropic", "OpenAI", "Google", "Cohere"]', TRUE),
('test-user-2', 'Speed Focused', 'Fastest response time', '["Google", "OpenAI", "Cohere", "Anthropic"]', TRUE),
('test-user-2', 'Balanced', 'Good balance of cost and quality', '["Cohere", "OpenAI", "Anthropic", "Google"]', FALSE);

-- Insert provider metrics for testing
INSERT INTO provider_metric (provider_id, metric_type, value, timestamp) VALUES
-- OpenAI metrics
(1, 'LATENCY', 1200.50, '2024-01-15 10:00:00'),
(1, 'THROUGHPUT', 95.20, '2024-01-15 10:00:00'),
(1, 'SUCCESS_RATE', 98.50, '2024-01-15 10:00:00'),
-- Anthropic metrics
(2, 'LATENCY', 1500.75, '2024-01-15 10:00:00'),
(2, 'THROUGHPUT', 88.90, '2024-01-15 10:00:00'),
(2, 'SUCCESS_RATE', 99.20, '2024-01-15 10:00:00'),
-- Google metrics
(3, 'LATENCY', 800.25, '2024-01-15 10:00:00'),
(3, 'THROUGHPUT', 92.15, '2024-01-15 10:00:00'),
(3, 'SUCCESS_RATE', 97.80, '2024-01-15 10:00:00'),
-- Cohere metrics
(4, 'LATENCY', 950.40, '2024-01-15 10:00:00'),
(4, 'THROUGHPUT', 89.60, '2024-01-15 10:00:00'),
(4, 'SUCCESS_RATE', 96.90, '2024-01-15 10:00:00');

-- Insert model quality metrics for testing
INSERT INTO model_metric (model_id, metric_type, metric_value, sample_size, timestamp) VALUES
-- GPT-4 quality metrics
(1, 'ACCURACY', 0.95, 1000, '2024-01-15 10:00:00'),
(1, 'FLUENCY', 0.92, 1000, '2024-01-15 10:00:00'),
(1, 'CREATIVITY', 0.88, 500, '2024-01-15 10:00:00'),
-- GPT-3.5 quality metrics
(2, 'ACCURACY', 0.89, 2000, '2024-01-15 10:00:00'),
(2, 'FLUENCY', 0.91, 2000, '2024-01-15 10:00:00'),
(2, 'CREATIVITY', 0.82, 1000, '2024-01-15 10:00:00'),
-- Claude 3 Opus quality metrics
(3, 'ACCURACY', 0.96, 800, '2024-01-15 10:00:00'),
(3, 'FLUENCY', 0.94, 800, '2024-01-15 10:00:00'),
(3, 'CREATIVITY', 0.91, 400, '2024-01-15 10:00:00'),
-- Claude 3 Sonnet quality metrics
(4, 'ACCURACY', 0.93, 1200, '2024-01-15 10:00:00'),
(4, 'FLUENCY', 0.92, 1200, '2024-01-15 10:00:00'),
(4, 'CREATIVITY', 0.87, 600, '2024-01-15 10:00:00'),
-- Gemini Pro quality metrics
(5, 'ACCURACY', 0.91, 1500, '2024-01-15 10:00:00'),
(5, 'FLUENCY', 0.89, 1500, '2024-01-15 10:00:00'),
(5, 'CREATIVITY', 0.85, 750, '2024-01-15 10:00:00'),
-- Command quality metrics
(7, 'ACCURACY', 0.87, 1000, '2024-01-15 10:00:00'),
(7, 'FLUENCY', 0.88, 1000, '2024-01-15 10:00:00'),
(7, 'CREATIVITY', 0.80, 500, '2024-01-15 10:00:00');

-- Insert sample AI requests for testing different scenarios
INSERT INTO ai_request (user_id, routing_strategy, prompt, max_tokens, temperature, selected_provider_id, selected_model_id, status, response, tokens_used, cost, latency_ms, error_message, created_at, completed_at) VALUES
-- Successful PRICE routing
('test-user-1', 'PRICE', 'Write a simple hello world program in Python', 100, 0.7, 1, 2, 'COMPLETED', 'Here is a simple hello world program in Python:\n\n```python\nprint("Hello, World!")\n```', 25, 0.000050, 1200, NULL, '2024-01-15 10:00:00', '2024-01-15 10:00:01'),
-- Successful LATENCY routing
('test-user-1', 'LATENCY', 'Explain quantum computing briefly', 200, 0.5, 3, 5, 'COMPLETED', 'Quantum computing uses quantum mechanics principles like superposition and entanglement to perform computations. Unlike classical bits (0 or 1), quantum bits (qubits) can exist in multiple states simultaneously, potentially solving certain problems much faster than classical computers.', 85, 0.000340, 800, NULL, '2024-01-15 10:05:00', '2024-01-15 10:05:00'),
-- Failed request (provider error)
('test-user-2', 'PRICE', 'Generate a complex algorithm', 500, 0.8, 1, 1, 'FAILED', NULL, NULL, NULL, 1500, 'Error: Model gpt-4 is currently overloaded', '2024-01-15 10:10:00', NULL),
-- Throughput routing (high volume)
('test-user-1', 'THROUGHPUT', 'Summarize this article about AI', 150, 0.3, 4, 7, 'COMPLETED', 'The article discusses recent advances in artificial intelligence, focusing on large language models and their applications in various industries. Key points include improved natural language processing capabilities and concerns about ethical AI development.', 67, 0.005050, 950, NULL, '2024-01-15 10:15:00', '2024-01-15 10:15:00'),
-- Custom routing rule usage
('test-user-1', 'CUSTOM_ORDER', 'Create a recipe for chocolate chip cookies', 300, 0.6, 2, 4, 'COMPLETED', 'Ingredients:\n- 2 1/4 cups all-purpose flour\n- 1 teaspoon baking soda\n- 1 teaspoon salt\n- 1 cup butter, softened\n- 3/4 cup granulated sugar\n- 3/4 cup brown sugar\n- 2 large eggs\n- 2 teaspoons vanilla extract\n- 2 cups chocolate chips\n\nInstructions:\n1. Preheat oven to 375°F (190°C)\n2. Mix dry ingredients\n3. Cream butter and sugars\n4. Add eggs and vanilla\n5. Combine wet and dry ingredients\n6. Fold in chocolate chips\n7. Drop spoonfuls onto baking sheet\n8. Bake for 9-11 minutes', 156, 0.002340, 1100, NULL, '2024-01-15 10:20:00', '2024-01-15 10:20:01'),
-- Pending request
('test-user-2', 'AUTO', 'Design a simple REST API', NULL, NULL, NULL, NULL, 'PENDING', NULL, NULL, NULL, NULL, NULL, '2024-01-15 10:25:00', NULL);

-- Insert routing decisions for testing
INSERT INTO routing_decision (request_id, strategy, considered_providers, selected_provider_id, selection_reason, fallback_used)
VALUES (1, 'PRICE', '["OpenAI", "Anthropic", "Google", "Cohere"]', 1, 'Selected OpenAI GPT-3.5-turbo as cheapest option with input_pricing=0.000002', FALSE);

INSERT INTO routing_decision (request_id, strategy, considered_providers, selected_provider_id, selection_reason, fallback_used)
VALUES (2, 'LATENCY', '["OpenAI", "Anthropic", "Google", "Cohere"]', 3, 'Selected Google Gemini Pro as fastest option with latency=800.25ms', FALSE);

INSERT INTO routing_decision (request_id, strategy, considered_providers, selected_provider_id, selection_reason, fallback_used)
VALUES (3, 'PRICE', '["OpenAI", "Anthropic", "Google", "Cohere"]', 1, 'Selected OpenAI GPT-4, but request failed due to provider overload', FALSE);

INSERT INTO routing_decision (request_id, strategy, considered_providers, selected_provider_id, selection_reason, fallback_used)
VALUES (4, 'THROUGHPUT', '["OpenAI", "Anthropic", "Google", "Cohere"]', 4, 'Selected Cohere Command for high throughput scenario', FALSE);

INSERT INTO routing_decision (request_id, strategy, considered_providers, selected_provider_id, selection_reason, fallback_used)
VALUES (5, 'CUSTOM_ORDER', '["Anthropic", "OpenAI", "Google", "Cohere"]', 2, 'Selected Anthropic Claude 3 Sonnet following custom priority order', FALSE);
