# PrismNet AI - Intelligent AI Routing Platform

PrismNet AI is a sophisticated AI routing platform that intelligently routes requests across multiple AI providers based on various strategies including cost, latency, throughput, and custom preferences.

## Features

- **Intelligent Routing**: Route requests based on cost, latency, throughput, or custom provider ordering
- **Multi-Provider Support**: Seamlessly integrate with multiple AI providers
- **Flexible Request Formats**: Support for various request structures to accommodate different routing needs
- **Fallback Mechanisms**: Automatic fallback to alternative providers when primary options fail
- **Real-time Metrics**: Track performance, cost, and latency across providers
- **Spring Boot Integration**: Built with Spring Boot for enterprise-grade reliability

## API Usage

### Chat Completions Endpoint

```
POST /v1/chat/completions
```

The API supports multiple flexible request formats for different routing scenarios:

#### 1. Single Model Routing (Direct)
Route to a specific model directly:

```json
{
  "model": "openai/gpt-4o",
  "messages": [
    {
      "role": "user",
      "content": "What is the meaning of life?"
    }
  ]
}
```

#### 2. Multiple Models with Fallback
Specify multiple models for fallback routing:

```json
{
  "models": ["anthropic/claude-3.5-sonnet", "gryphe/mythomax-l2-13b"],
  "messages": [
    {
      "role": "user",
      "content": "What is the meaning of life?"
    }
  ]
}
```

#### 3. Provider-Optimized Routing
Route based on provider performance criteria:

```json
{
  "model": "meta-llama/llama-3.1-70b-instruct",
  "messages": [
    {
      "role": "user",
      "content": "Hello"
    }
  ],
  "provider": {
    "sort": "throughput"
  }
}
```

#### 4. Custom Provider Ordering
Specify exact provider priority order:

```json
{
  "model": "mistralai/mixtral-8x7b-instruct",
  "messages": [
    {
      "role": "user",
      "content": "Hello"
    }
  ],
  "provider": {
    "order": ["openai", "together"],
    "allow_fallbacks": false
  }
}
```

#### 5. Legacy Routing Strategy (Backward Compatible)
Continue using explicit routing strategies:

```json
{
  "routingStrategy": "PRICE",
  "messages": [
    {
      "role": "user",
      "content": "What is the meaning of life?"
    }
  ]
}
```

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `model` | string | No* | Specific model to route to |
| `models` | array | No* | List of models for fallback routing |
| `provider` | object | No* | Provider-specific configuration |
| `routingStrategy` | string | No* | Legacy routing strategy (PRICE, LATENCY, THROUGHPUT, AUTO, CUSTOM_ORDER, PREFERRED_MODEL) |
| `messages` | array | Yes | Chat messages |
| `maxTokens` | integer | No | Maximum tokens to generate (default: 100) |
| `temperature` | number | No | Sampling temperature (0.0-2.0, default: 1.0) |
| `stream` | boolean | No | Enable streaming response (default: false) |

*At least one routing configuration (`model`, `models`, `provider`, or `routingStrategy`) must be provided.

### Provider Options

The `provider` object supports the following configuration:

| Field | Type | Description |
|-------|------|-------------|
| `sort` | string | Sort criteria: "throughput", "latency", "price", "cost" |
| `order` | array | Custom provider priority order |
| `allowFallbacks` | boolean | Whether to allow fallbacks to other providers |

### Response Format

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-3.5-turbo",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "The meaning of life is..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 13,
    "completion_tokens": 7,
    "total_tokens": 20
  }
}
```

### API Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
  - Interactive API documentation with try-it-out functionality
  - JWT authentication support for testing endpoints
  - Complete OpenAPI 3.0 specification

### Monitoring
- Health check: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`



### Docker - DB
# Build
docker build -f docker/prismnetai-db.Dockerfile -t prismnetai-db:latest .

# Test
# Stop & remove any existing container named kingston-db
docker rm -f prismnetai-db || true

# Run the container (mapping port 3306); because the SQL scripts are included,
# they will run on first startup to create/populate the `kingston` database.
docker run -d \
  --name prismnetai-db \
  -e MYSQL_ROOT_PASSWORD='t!g3rP_k' \
  -e MYSQL_DATABASE='kingston' \
  -e MYSQL_USER='kingston' \
  -e MYSQL_PASSWORD='t!g3rP_k' \
  -p 3306:3306 \
  prismnetai-db:latest


### Docker Build - Web

### Docker Run - Web