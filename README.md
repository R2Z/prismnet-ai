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

## Routing Strategies

### Built-in Strategies

1. **PRICE**: Route to the cheapest available provider
2. **LATENCY**: Route to the fastest provider
3. **THROUGHPUT**: Route to the highest throughput provider
4. **AUTO**: Automatic routing based on current metrics
5. **CUSTOM_ORDER**: Route according to specified provider order
6. **PREFERRED_MODEL**: Route to a specific preferred model

### Inference Logic

The system automatically infers the routing strategy based on request structure:

- Single `model` → PREFERRED_MODEL strategy
- Multiple `models` → CUSTOM_ORDER strategy with fallbacks
- `provider.sort` → Maps to corresponding strategy (throughput → THROUGHPUT)
- `provider.order` → CUSTOM_ORDER strategy
- No routing config → AUTO strategy

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- Database (PostgreSQL/MySQL recommended)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/your-org/prismnet-ai.git
cd prismnet-ai
```

2. Configure your database in `application.properties`

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

### Configuration

Configure AI providers in your `application.properties`:

```properties
# OpenAI Configuration
openai.api.key=your-openai-api-key
openai.api.url=https://api.openai.com/v1

# Anthropic Configuration
anthropic.api.key=your-anthropic-api-key
anthropic.api.url=https://api.anthropic.com

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/prismnet
spring.datasource.username=your-username
spring.datasource.password=your-password
```

## Architecture

### Core Components

- **ChatCompletionController**: REST API endpoint handling chat completion requests
- **RoutingService**: Core routing logic and strategy execution
- **RoutingStrategyInferenceService**: Intelligent routing strategy inference from request format
- **Provider Services**: Individual provider implementations (OpenAI, Anthropic, etc.)
- **Metrics Service**: Performance tracking and analytics

### Request Flow

1. **Request Validation**: Validate request structure and parameters
2. **Strategy Inference**: Determine routing strategy from request format
3. **Provider Selection**: Select optimal provider based on strategy
4. **Request Execution**: Forward request to selected provider
5. **Response Handling**: Process and return response with routing metadata

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Contact the development team
- Check the documentation for common solutions
