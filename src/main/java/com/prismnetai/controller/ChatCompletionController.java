package com.prismnetai.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;
import com.prismnetai.service.RoutingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/chat/completions")
@RequiredArgsConstructor
@Tag(name = "Chat Completions", description = "AI chat completion endpoints with intelligent routing")
public class ChatCompletionController {

    private final RoutingService routingService;
    private final WebClient webClient;

    @PostMapping
    @Operation(summary = "Create chat completion with routing",
               description = "Submit a chat completion request that will be routed based on the specified strategy")
    public ResponseEntity<ChatCompletionResponse> createCompletion(
            @RequestBody ChatCompletionRequest request,
            Authentication authentication) {

        log.info("ChatCompletionController.createCompletion() - Received chat completion request with routing strategy: {}, messageCount: {}",
                 request.getRoutingStrategy(), request.getMessages() != null ? request.getMessages().size() : 0);

        // Extract user ID from authentication
        String userId = authentication.getName();
        log.debug("ChatCompletionController.createCompletion() - Processing request for authenticated user: {}", userId);

        // Validate request parameters
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            log.warn("ChatCompletionController.createCompletion() - Request validation failed: no messages provided");
            throw new IllegalArgumentException("Messages are required");
        }

        if (request.getRoutingStrategy() == null || request.getRoutingStrategy().trim().isEmpty()) {
            log.warn("ChatCompletionController.createCompletion() - Request validation failed: no routing strategy provided");
            throw new IllegalArgumentException("Routing strategy is required");
        }

        String prompt = extractPrompt(request);
        log.debug("ChatCompletionController.createCompletion() - Extracted prompt length: {} characters", prompt.length());

        // Route the request
        log.info("ChatCompletionController.createCompletion() - Routing request for user: {} with strategy: {}",
                 userId, request.getRoutingStrategy());

        AiRequest aiRequest = routingService.routeRequest(
            userId,
            AiRequest.RoutingStrategy.valueOf(request.getRoutingStrategy().toUpperCase()),
            prompt,
            request.getMaxTokens()
        );

        log.info("ChatCompletionController.createCompletion() - Request routed successfully, requestId: {}, selectedModel: {}, selectedProvider: {}",
                 aiRequest.getId(), aiRequest.getSelectedModel().getModelId(), aiRequest.getSelectedProvider().getName());

        // Check if the selected provider is OpenAI
        if ("OpenAI".equals(aiRequest.getSelectedProvider().getName())) {
            log.info("ChatCompletionController.createCompletion() - OpenAI provider detected, calling OpenAI API");
            return callOpenAICompletion(request, aiRequest);
        }

        // Check if the selected provider is Anthropic
        if ("Anthropic".equals(aiRequest.getSelectedProvider().getName())) {
            log.info("ChatCompletionController.createCompletion() - Anthropic provider detected, calling Anthropic API");
            return callAnthropicCompletion(request, aiRequest);
        }

        // For non-OpenAI/Anthropic providers, return a placeholder response
        // In a full implementation, this would call the actual AI provider
        ChatCompletionResponse response = ChatCompletionResponse.builder()
            .id("chatcmpl-" + aiRequest.getId())
            .object("chat.completion")
            .created((int)(System.currentTimeMillis() / 1000))
            .model(aiRequest.getSelectedModel().getModelId())
            .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                .strategy(request.getRoutingStrategy())
                .provider(aiRequest.getSelectedProvider().getName())
                .costSavings(BigDecimal.valueOf(0.0)) // Would be calculated based on routing
                .latencyMs(0L)    // Would be measured
                .build())
            .choices(java.util.List.of(
                ChatCompletionResponse.ChatChoice.builder()
                    .index(0)
                    .message(ChatCompletionResponse.ChatMessage.builder()
                        .role("assistant")
                        .content("This is a placeholder response. Price routing to " +
                                aiRequest.getSelectedProvider().getName() + " successful.")
                        .build())
                    .finishReason("stop")
                    .build()
            ))
            .usage(ChatCompletionResponse.Usage.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .cost(BigDecimal.valueOf(0.001))
                .build())
            .build();

        log.info("ChatCompletionController.createCompletion() - Successfully processed request for user: {}, returning response with id: {}",
                 userId, response.getId());

        return ResponseEntity.ok(response);
    }

    private String extractPrompt(ChatCompletionRequest request) {
        log.debug("ChatCompletionController.extractPrompt() - Extracting user message from {} messages",
                  request.getMessages().size());

        String prompt = request.getMessages().stream()
            .filter(msg -> "user".equals(msg.getRole()))
            .findFirst()
            .map(ChatCompletionRequest.ChatMessage::getContent)
            .orElse("No user message found");

        log.debug("ChatCompletionController.extractPrompt() - Extracted prompt: {}...",
                  prompt.substring(0, Math.min(50, prompt.length())));

        return prompt;
    }

    private ResponseEntity<ChatCompletionResponse> callOpenAICompletion(ChatCompletionRequest request, AiRequest aiRequest) {
        log.debug("ChatCompletionController.callOpenAICompletion() - Calling OpenAI completions API for request: {}", aiRequest.getId());

        try {
            // Build OpenAI request body
            var openAiRequest = buildOpenAIRequest(request, aiRequest);

            // Make HTTP call to OpenAI
            var openAiResponse = webClient.post()
                .uri(aiRequest.getSelectedProvider().getBaseUrl() + "/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiRequest.getSelectedProvider().getApiKey())
                .bodyValue(openAiRequest)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // Parse response and build ChatCompletionResponse
            return ResponseEntity.ok(parseOpenAIResponse(openAiResponse, aiRequest, request.getRoutingStrategy()));

        } catch (Exception e) {
            log.error("ChatCompletionController.callOpenAICompletion() - Error calling OpenAI API: {}", e.getMessage(), e);
            // Fallback to placeholder response on error
            return createPlaceholderResponse(aiRequest, request.getRoutingStrategy());
        }
    }

    private Object buildOpenAIRequest(ChatCompletionRequest request, AiRequest aiRequest) {
        String prompt = extractPrompt(request);

        return java.util.Map.of(
            "model", aiRequest.getSelectedModel().getModelId(),
            "prompt", prompt,
            "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 100,
            "temperature", request.getTemperature() != null ? request.getTemperature() : BigDecimal.valueOf(1.0)
        );
    }

    private ChatCompletionResponse parseOpenAIResponse(String openAiResponse, AiRequest aiRequest, String routingStrategy) {
        // Parse the JSON response from OpenAI
        // For now, create a simple implementation - in production you'd use Jackson or similar
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(openAiResponse);

            String content = jsonNode.path("choices").get(0).path("text").asText();
            int promptTokens = jsonNode.path("usage").path("prompt_tokens").asInt();
            int completionTokens = jsonNode.path("usage").path("completion_tokens").asInt();

            return ChatCompletionResponse.builder()
                .id("chatcmpl-" + aiRequest.getId())
                .object("chat.completion")
                .created((int)(System.currentTimeMillis() / 1000))
                .model(aiRequest.getSelectedModel().getModelId())
                .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                    .strategy(routingStrategy)
                    .provider(aiRequest.getSelectedProvider().getName())
                    .costSavings(BigDecimal.valueOf(0.0))
                    .latencyMs(0L)
                    .build())
                .choices(java.util.List.of(
                    ChatCompletionResponse.ChatChoice.builder()
                        .index(0)
                        .message(ChatCompletionResponse.ChatMessage.builder()
                            .role("assistant")
                            .content(content)
                            .build())
                        .finishReason("stop")
                        .build()
                ))
                .usage(ChatCompletionResponse.Usage.builder()
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .cost(BigDecimal.valueOf(0.001)) // Would calculate based on pricing
                    .build())
                .build();

        } catch (JsonProcessingException e) {
            log.error("ChatCompletionController.parseOpenAIResponse() - Error parsing OpenAI response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    private ResponseEntity<ChatCompletionResponse> callAnthropicCompletion(ChatCompletionRequest request, AiRequest aiRequest) {
        log.debug("ChatCompletionController.callAnthropicCompletion() - Calling Anthropic messages API for request: {}", aiRequest.getId());

        try {
            // Build Anthropic request body
            var anthropicRequest = buildAnthropicRequest(request, aiRequest);

            // Make HTTP call to Anthropic
            var anthropicResponse = webClient.post()
                .uri(aiRequest.getSelectedProvider().getBaseUrl() + "/messages")
                .header("Content-Type", "application/json")
                .header("x-api-key", aiRequest.getSelectedProvider().getApiKey())
                .header("anthropic-version", "2023-06-01")
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // Parse response and build ChatCompletionResponse
            return ResponseEntity.ok(parseAnthropicResponse(anthropicResponse, aiRequest, request.getRoutingStrategy()));

        } catch (Exception e) {
            log.error("ChatCompletionController.callAnthropicCompletion() - Error calling Anthropic API: {}", e.getMessage(), e);
            // Fallback to placeholder response on error
            return createPlaceholderResponse(aiRequest, request.getRoutingStrategy());
        }
    }

    private Object buildAnthropicRequest(ChatCompletionRequest request, AiRequest aiRequest) {
        // Extract messages from request
        var messages = request.getMessages().stream()
            .map(msg -> java.util.Map.of(
                "role", msg.getRole(),
                "content", msg.getContent()
            ))
            .toList();

        return java.util.Map.of(
            "model", aiRequest.getSelectedModel().getModelId(),
            "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1024,
            "messages", messages,
            "temperature", request.getTemperature() != null ? request.getTemperature() : BigDecimal.valueOf(1.0)
        );
    }

    private ChatCompletionResponse parseAnthropicResponse(String anthropicResponse, AiRequest aiRequest, String routingStrategy) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(anthropicResponse);

            String content = jsonNode.path("content").get(0).path("text").asText();
            int inputTokens = jsonNode.path("usage").path("input_tokens").asInt();
            int outputTokens = jsonNode.path("usage").path("output_tokens").asInt();

            return ChatCompletionResponse.builder()
                .id("chatcmpl-" + aiRequest.getId())
                .object("chat.completion")
                .created((int)(System.currentTimeMillis() / 1000))
                .model(aiRequest.getSelectedModel().getModelId())
                .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                    .strategy(routingStrategy)
                    .provider(aiRequest.getSelectedProvider().getName())
                    .costSavings(BigDecimal.valueOf(0.0))
                    .latencyMs(0L)
                    .build())
                .choices(java.util.List.of(
                    ChatCompletionResponse.ChatChoice.builder()
                        .index(0)
                        .message(ChatCompletionResponse.ChatMessage.builder()
                            .role("assistant")
                            .content(content)
                            .build())
                        .finishReason("stop")
                        .build()
                ))
                .usage(ChatCompletionResponse.Usage.builder()
                    .promptTokens(inputTokens)
                    .completionTokens(outputTokens)
                    .totalTokens(inputTokens + outputTokens)
                    .cost(BigDecimal.valueOf(0.001)) // Would calculate based on pricing
                    .build())
                .build();

        } catch (JsonProcessingException e) {
            log.error("ChatCompletionController.parseAnthropicResponse() - Error parsing Anthropic response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Anthropic response", e);
        }
    }

    private ResponseEntity<ChatCompletionResponse> createPlaceholderResponse(AiRequest aiRequest, String routingStrategy) {
        ChatCompletionResponse response = ChatCompletionResponse.builder()
            .id("chatcmpl-" + aiRequest.getId())
            .object("chat.completion")
            .created((int)(System.currentTimeMillis() / 1000))
            .model(aiRequest.getSelectedModel().getModelId())
            .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                .strategy(routingStrategy)
                .provider(aiRequest.getSelectedProvider().getName())
                .costSavings(BigDecimal.valueOf(0.0))
                .latencyMs(0L)
                .build())
            .choices(java.util.List.of(
                ChatCompletionResponse.ChatChoice.builder()
                    .index(0)
                    .message(ChatCompletionResponse.ChatMessage.builder()
                        .role("assistant")
                        .content("This is a placeholder response. API call failed.")
                        .build())
                    .finishReason("stop")
                    .build()
            ))
            .usage(ChatCompletionResponse.Usage.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .cost(BigDecimal.valueOf(0.001))
                .build())
            .build();

        return ResponseEntity.ok(response);
    }
}