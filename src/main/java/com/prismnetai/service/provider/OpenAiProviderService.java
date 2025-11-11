package com.prismnetai.service.provider;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;
import com.prismnetai.exception.ProviderException;
import com.prismnetai.service.provider.client.OpenAiApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for handling OpenAI API interactions.
 * This class encapsulates all OpenAI-specific logic including request building,
 * API calls, and response parsing.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiProviderService implements AiProviderService {

    private static final String PROVIDER_NAME = "OpenAI";

    private final OpenAiApiClient openAiApiClient;
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

        log.info("OpenAiProviderService.callCompletion() - Calling OpenAI API for requestId: {}", aiRequest.getId());

        Instant startTime = Instant.now();

        try {
            Map<String, Object> openAiRequest = buildOpenAiRequest(request, aiRequest);
            log.info("OpenAI request JSON: {}", objectMapper.writeValueAsString(openAiRequest));
            String responseBody = openAiApiClient.chatCompletions(openAiRequest, aiRequest.getSelectedProvider().getBaseUrl(), aiRequest.getSelectedProvider().getApiKey());
            log.info("OpenAI response JSON: {}", responseBody);
            ChatCompletionResponse response = parseOpenAiResponse(responseBody, aiRequest, request);

            long latencyMs = Duration.between(startTime, Instant.now()).toMillis();
            log.info("OpenAiProviderService.callCompletion() - Successfully processed OpenAI request in {}ms", latencyMs);

            return response;

        } catch (WebClientException e) {
            log.error("OpenAiProviderService.callCompletion() - WebClient error calling OpenAI API: {}", e.getMessage(), e);
            throw new ProviderException("Failed to communicate with OpenAI API", e);
        } catch (JsonProcessingException e) {
            log.error("OpenAiProviderService.callCompletion() - JSON processing error: {}", e.getMessage(), e);
            throw new ProviderException("Failed to parse OpenAI response", e);
        } catch (Exception e) {
            log.error("OpenAiProviderService.callCompletion() - Unexpected error calling OpenAI API: {}", e.getMessage(), e);
            throw new ProviderException("Unexpected error occurred while calling OpenAI API", e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Builds the OpenAI API request payload from the chat completion request.
     *
     * @param request the chat completion request
     * @param aiRequest the AI request entity
     * @return the OpenAI request payload as a map
     */
    private Map<String, Object> buildOpenAiRequest(ChatCompletionRequest request, AiRequest aiRequest) {
        log.info("OpenAiProviderService.buildOpenAiRequest() - Building OpenAI request for model: {}", aiRequest.getSelectedModel().getModelId());

        return Map.of(
            "model", aiRequest.getSelectedModel().getModelId(),
            "messages", request.getMessages(),
            "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 100,
            "temperature", request.getTemperature() != null ? request.getTemperature() : BigDecimal.valueOf(1.0)
        );
    }


    /**
     * Parses the OpenAI API response into a ChatCompletionResponse.
     *
     * @param responseBody the raw response body
     * @param aiRequest the AI request entity
     * @param originalRequest the original chat completion request
     * @return the parsed chat completion response
     * @throws JsonProcessingException if JSON parsing fails
     */
    private ChatCompletionResponse parseOpenAiResponse(String responseBody, AiRequest aiRequest, ChatCompletionRequest originalRequest)
            throws JsonProcessingException {

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
        int promptTokens = jsonNode.path("usage").path("prompt_tokens").asInt();
        int completionTokens = jsonNode.path("usage").path("completion_tokens").asInt();

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
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .cost(BigDecimal.valueOf(0.001)) // Would calculate based on pricing
                .build())
            .build();
    }

    /**
     * Extracts the user prompt from the chat completion request.
     *
     * @param request the chat completion request
     * @return the extracted prompt
     */
    private String extractPrompt(ChatCompletionRequest request) {
        return request.getMessages().stream()
            .filter(msg -> "user".equals(msg.getRole()))
            .findFirst()
            .map(ChatCompletionRequest.ChatMessage::getContent)
            .orElse("No user message found");
    }
}