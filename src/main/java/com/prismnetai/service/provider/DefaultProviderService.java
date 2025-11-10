package com.prismnetai.service.provider;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Default provider service that handles providers without specific implementations.
 * This service provides placeholder responses for providers that don't have dedicated services.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
public class DefaultProviderService implements AiProviderService {

    private static final String PROVIDER_NAME = "Default";

    @Override
    public boolean canHandle(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }
        // This service can handle any provider that doesn't have a specific implementation
        return !"OpenAI".equalsIgnoreCase(providerName) && !"Anthropic".equalsIgnoreCase(providerName);
    }

    @Override
    public ChatCompletionResponse callCompletion(ChatCompletionRequest request, AiRequest aiRequest) {
        if (request == null) {
            throw new IllegalArgumentException("ChatCompletionRequest cannot be null");
        }
        if (aiRequest == null) {
            throw new IllegalArgumentException("AiRequest cannot be null");
        }

        log.info("DefaultProviderService.callCompletion() - Using default provider service for provider: {}, requestId: {}",
                aiRequest.getSelectedProvider().getName(), aiRequest.getId());

        return createPlaceholderResponse(aiRequest, request);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Creates a placeholder response for providers without specific implementations.
     *
     * @param aiRequest the AI request entity
     * @param request the original chat completion request
     * @return a placeholder chat completion response
     */
    private ChatCompletionResponse createPlaceholderResponse(AiRequest aiRequest, ChatCompletionRequest request) {
        return ChatCompletionResponse.builder()
            .id("chatcmpl-" + aiRequest.getId())
            .object("chat.completion")
            .created((int) (System.currentTimeMillis() / 1000))
            .model(aiRequest.getSelectedModel().getModelId())
            .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                .strategy(request.getRoutingStrategy())
                .provider(aiRequest.getSelectedProvider().getName())
                .costSavings(BigDecimal.ZERO)
                .latencyMs(0L)
                .build())
            .choices(java.util.List.of(
                ChatCompletionResponse.ChatChoice.builder()
                    .index(0)
                    .message(ChatCompletionResponse.ChatMessage.builder()
                        .role("assistant")
                        .content("This is a placeholder response. Provider routing to " +
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
    }
}