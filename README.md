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

**For Development/Testing:**
- The application currently accepts any username in JWT tokens (no password validation)
- You can use any string as an API key for testing
- **Important**: The JWT secret key must be at least 256 bits (32 characters) for HMAC-SHA algorithms

**⚠️ SECURITY WARNING:**
- The current authentication setup is for development/testing only (profiles: local, dev, test)
- **DO NOT deploy to production** with the default JWT secret or simple API key validation
- In production profiles, only proper JWT tokens will be accepted
- Implement proper user authentication, password validation, and use environment variables for secrets
- Generate a secure random 256-bit (32+ character) JWT secret for production

```bash
# JWT (recommended for users)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" ...

# API Key (for service accounts)
curl -H "X-API-Key: YOUR_API_KEY" ...
```

**To get a Bearer token for Postman:**
1. Use Swagger UI at `http://localhost:8080/swagger-ui.html`
2. Click "Authorize" button in the top right
3. Enter any username (e.g., "test-user") in the JWT field
4. Click "Authorize" - this will set the token for all requests
5. Or manually set header: `Authorization: Bearer test-user`

**Alternative: Use API Key header instead of JWT:**
- Set header: `X-API-Key: test-user` (any non-empty string works for testing)
- Or use Bearer header with simple string: `Authorization: Bearer test-user`

**Required Headers for /v1/chat/completions:**
- `Authorization: Bearer <token>` (or `X-API-Key: <key>`)
- `Content-Type: application/json`

#### Route by Price (Default)
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routingStrategy": "PRICE",
    "messages": [
      {
        "role": "user",
        "content": "Hello, how are you?"
      }
    ],
    "maxTokens": 100
  }'
```

#### Route by Latency
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routingStrategy": "LATENCY",
    "messages": [
      {
        "role": "user",
        "content": "Explain quantum computing in simple terms"
      }
    ],
    "maxTokens": 200,
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
    "providerOrder": ["Anthropic", "OpenAI"]
  }'
```

Then use it in requests:
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routingStrategy": "CUSTOM_ORDER",
    "routingRuleId": "rule-456",
    "messages": [
      {
        "role": "user",
        "content": "Write a haiku about artificial intelligence"
      }
    ],
    "maxTokens": 50
  }'
```

#### Auto Router
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "routingStrategy": "AUTO",
    "messages": [
      {
        "role": "user",
        "content": "Create a complex algorithm for sorting a list"
      }
    ],
    "maxTokens": 300
  }'
```

## Routing Strategy Samples

Below are sample JSON request strings for each routing strategy. These can be used in the request body for POST `/v1/chat/completions`.

### PRICE
```json
{
 "routingStrategy": "PRICE",
 "messages": [
   {
     "role": "user",
     "content": "Hello, how are you?"
   }
 ],
 "maxTokens": 100,
 "temperature": 1.0,
 "stream": false
}
```

### THROUGHPUT
```json
{
 "routingStrategy": "THROUGHPUT",
 "messages": [
   {
     "role": "user",
     "content": "Generate a list of 10 random numbers."
   }
 ],
 "maxTokens": 150,
 "temperature": 0.5,
 "stream": false
}
```

### LATENCY
```json
{
 "routingStrategy": "LATENCY",
 "messages": [
   {
     "role": "user",
     "content": "Explain quantum computing in simple terms."
   }
 ],
 "maxTokens": 200,
 "temperature": 0.7,
 "stream": false
}
```

### CUSTOM_ORDER
```json
{
 "routingStrategy": "CUSTOM_ORDER",
 "routingRuleId": "rule-456",
 "messages": [
   {
     "role": "user",
     "content": "Write a haiku about artificial intelligence."
   }
 ],
 "maxTokens": 50,
 "temperature": 1.0,
 "stream": false
}
```

### AUTO
```json
{
 "routingStrategy": "AUTO",
 "messages": [
   {
     "role": "user",
     "content": "Create a complex algorithm for sorting a list."
   }
 ],
 "maxTokens": 300,
 "temperature": 0.8,
 "stream": false
}
```

### PREFERRED_MODEL
```json
{
 "routingStrategy": "PREFERRED_MODEL",
 "preferredModel": "gpt-4",
 "messages": [
   {
     "role": "user",
     "content": "Summarize the benefits of renewable energy."
   }
 ],
 "maxTokens": 250,
 "temperature": 0.9,
 "stream": false
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
