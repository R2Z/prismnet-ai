package com.prismnetai.service.provider;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;

/**
 * Interface for AI provider services that handle communication with different AI providers.
 * This interface follows the Interface Segregation Principle by defining only the methods
 * needed for AI provider interactions.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
public interface AiProviderService {

    /**
     * Determines if this service can handle the given provider.
     *
     * @param providerName the name of the provider to check
     * @return true if this service can handle the provider, false otherwise
     * @throws IllegalArgumentException if providerName is null or empty
     */
    boolean canHandle(String providerName);

    /**
     * Calls the AI provider's API to process a chat completion request.
     *
     * @param request the chat completion request containing messages and parameters
     * @param aiRequest the AI request entity containing routing information
     * @return the chat completion response from the provider
     * @throws ProviderException if the provider API call fails
     * @throws IllegalArgumentException if request or aiRequest is null
     */
    ChatCompletionResponse callCompletion(ChatCompletionRequest request, AiRequest aiRequest);

    /**
     * Calls the AI provider's API to process a streaming chat completion request.
     *
     * @param request the chat completion request containing messages and parameters
     * @param aiRequest the AI request entity containing routing information
     * @return a Flux of streaming response chunks from the provider
     * @throws ProviderException if the provider API call fails
     * @throws IllegalArgumentException if request or aiRequest is null
     */
    reactor.core.publisher.Flux<String> callStreamingCompletion(ChatCompletionRequest request, AiRequest aiRequest);

    /**
     * Returns the name of the provider this service handles.
     *
     * @return the provider name
     */
    String getProviderName();
}