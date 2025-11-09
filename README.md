# PrismNet AI

A unified API layer for routing AI requests across hundreds of models from multiple providers, enabling intelligent routing by price, throughput, latency, custom ordering, and auto-optimization.

## Features

### Core Routing Strategies
- **Price Routing**: Automatically route to the cheapest available provider
- **Throughput Routing**: Prioritize providers with highest throughput for high-volume requests
- **Latency Routing**: Route to providers with lowest latency for real-time applications
- **Custom Provider Ordering**: Define specific provider preferences and priorities
- **Auto Router**: Optimize prompts across models for best output quality

### Key Capabilities
- Unified RESTful API across all providers
- Support for hundreds of AI models
- Real-time performance monitoring and metrics
- Automatic failover and provider availability handling
- Concurrent request support with different routing preferences
- Comprehensive logging and observability

## Quick Start

### Basic Usage

#### Authentication
All requests require authentication using JWT or API key:
```bash
# JWT (recommended for users)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" ...

# API Key (for service accounts)
curl -H "X-API-Key: YOUR_API_KEY" ...
```

#### Route by Price (Default)
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routing_strategy": "PRICE",
    "messages": [
      {
        "role": "user",
        "content": "Hello, how are you?"
      }
    ],
    "max_tokens": 100
  }'
```

#### Route by Latency
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routing_strategy": "LATENCY",
    "messages": [
      {
        "role": "user",
        "content": "Explain quantum computing in simple terms"
      }
    ],
    "max_tokens": 200,
    "temperature": 0.7
  }'
```

#### Custom Provider Ordering
First, create a routing rule:
```bash
curl -X POST http://localhost:8080/v1/routing/rules \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Preferred Order",
    "description": "Prioritize Anthropic, then OpenAI",
    "provider_order": ["Anthropic", "OpenAI"]
  }'
```

Then use it in requests:
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routing_strategy": "CUSTOM_ORDER",
    "routing_rule_id": "rule-456",
    "messages": [
      {
        "role": "user",
        "content": "Write a haiku about artificial intelligence"
      }
    ],
    "max_tokens": 50
  }'
```

#### Auto Router
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routing_strategy": "AUTO",
    "messages": [
      {
        "role": "user",
        "content": "Create a complex algorithm for sorting a list"
      }
    ],
    "max_tokens": 300
  }'
```

### Monitoring
- Health check: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`
