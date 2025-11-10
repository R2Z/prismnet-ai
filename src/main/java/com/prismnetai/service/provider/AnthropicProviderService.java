package com.prismnetai.service.provider;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;
import com.prismnetai.exception.ProviderException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for handling Anthropic API interactions.
 * This class encapsulates all Anthropic-specific logic including request building,
 * API calls, and response parsing.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnthropicProviderService implements AiProviderService {

    private static final String PROVIDER_NAME = "Anthropic";
    private static final String MESSAGES_ENDPOINT = "/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }
        return PROVIDER_NAME.equalsIgnoreCase(providerName);
    }

    @Override
    public ChatCompletionResponse callCompletion(ChatCompletionRequest request, AiRequest aiRequest) {
        if (request == null) {
            throw new IllegalArgumentException("ChatCompletionRequest cannot be null");
        }
        if (aiRequest == null) {
            throw new IllegalArgumentException("AiRequest cannot be null");
        }

        log.info("AnthropicProviderService.callCompletion() - Calling Anthropic API for requestId: {}", aiRequest.getId());

        Instant startTime = Instant.now();

        try {
            Map<String, Object> anthropicRequest = buildAnthropicRequest(request, aiRequest);
            String responseBody = makeApiCall(anthropicRequest, aiRequest);
            ChatCompletionResponse response = parseAnthropicResponse(responseBody, aiRequest, request);

            long latencyMs = Duration.between(startTime, Instant.now()).toMillis();
            log.info("AnthropicProviderService.callCompletion() - Successfully processed Anthropic request in {}ms", latencyMs);

            return response;

        } catch (WebClientException e) {
            log.error("AnthropicProviderService.callCompletion() - WebClient error calling Anthropic API: {}", e.getMessage(), e);
            throw new ProviderException("Failed to communicate with Anthropic API", e);
        } catch (JsonProcessingException e) {
            log.error("AnthropicProviderService.callCompletion() - JSON processing error: {}", e.getMessage(), e);
            throw new ProviderException("Failed to parse Anthropic response", e);
        } catch (Exception e) {
            log.error("AnthropicProviderService.callCompletion() - Unexpected error calling Anthropic API: {}", e.getMessage(), e);
            throw new ProviderException("Unexpected error occurred while calling Anthropic API", e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Builds the Anthropic API request payload from the chat completion request.
     *
     * @param request the chat completion request
     * @param aiRequest the AI request entity
     * @return the Anthropic request payload as a map
     */
    private Map<String, Object> buildAnthropicRequest(ChatCompletionRequest request, AiRequest aiRequest) {
        log.info("AnthropicProviderService.buildAnthropicRequest() - Building Anthropic request for model: {}", aiRequest.getSelectedModel().getModelId());

        List<Map<String, String>> messages = request.getMessages().stream()
            .map(msg -> Map.of(
                "role", msg.getRole(),
                "content", msg.getContent()
            ))
            .toList();

        return Map.of(
            "model", aiRequest.getSelectedModel().getModelId(),
            "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1024,
            "messages", messages,
            "temperature", request.getTemperature() != null ? request.getTemperature() : BigDecimal.valueOf(1.0)
        );
    }

    /**
     * Makes the actual HTTP call to the Anthropic API.
     * This method handles the HTTP communication and includes proper timeout handling.
     *
     * @param requestPayload the request payload
     * @param aiRequest the AI request entity
     * @return the response body as a string
     * @throws WebClientException if the HTTP call fails
     */
    private String makeApiCall(Map<String, Object> requestPayload, AiRequest aiRequest) {
        try {
            return webClient.post()
                .uri(aiRequest.getSelectedProvider().getBaseUrl() + MESSAGES_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", aiRequest.getSelectedProvider().getApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(API_TIMEOUT)
                .block();
        } catch (Exception e) {
            log.error("AnthropicProviderService.makeApiCall() - Failed to make API call to Anthropic: {}", e.getMessage());
            throw e; // Re-throw to be handled by caller
        }
    }

    /**
     * Parses the Anthropic API response into a ChatCompletionResponse.
     *
     * @param responseBody the raw response body
     * @param aiRequest the AI request entity
     * @param originalRequest the original chat completion request
     * @return the parsed chat completion response
     * @throws JsonProcessingException if JSON parsing fails
     */
    private ChatCompletionResponse parseAnthropicResponse(String responseBody, AiRequest aiRequest, ChatCompletionRequest originalRequest)
            throws JsonProcessingException {

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        String content = jsonNode.path("content").get(0).path("text").asText();
        int inputTokens = jsonNode.path("usage").path("input_tokens").asInt();
        int outputTokens = jsonNode.path("usage").path("output_tokens").asInt();

        return ChatCompletionResponse.builder()
            .id("chatcmpl-" + aiRequest.getId())
            .object("chat.completion")
            .created((int) (System.currentTimeMillis() / 1000))
            .model(aiRequest.getSelectedModel().getModelId())
            .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                .strategy(originalRequest.getRoutingStrategy())
                .provider(PROVIDER_NAME)
                .costSavings(BigDecimal.ZERO) // Would be calculated based on routing
                .latencyMs(0L) // Would be measured
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
    }
}