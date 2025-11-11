package com.prismnetai.service.provider.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client service for handling Anthropic API HTTP calls.
 * This class encapsulates the HTTP communication logic specific to Anthropic API.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnthropicApiClient {

    private static final String MESSAGES_ENDPOINT = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    /**
     * Makes an HTTP call to the Anthropic messages API.
     *
     * @param requestPayload the request payload
     * @param baseUrl the base URL of the provider
     * @param apiKey the API key for authentication
     * @return the response body as a string
     * @throws Exception if the HTTP call fails
     */
    public String messages(Map<String, Object> requestPayload, String baseUrl, String apiKey) {
        try {
            String fullUrl = baseUrl + MESSAGES_ENDPOINT;

            log.info("AnthropicApiClient.callApi() - Headers: Content-Type={}, x-api-key=[REDACTED], anthropic-version={}",
                MediaType.APPLICATION_JSON_VALUE,
                ANTHROPIC_VERSION);

            return webClient.post()
                .uri(fullUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(API_TIMEOUT)
                .block();
        } catch (Exception e) {
            log.error("AnthropicApiClient.callApi() - Failed to make API call to Anthropic: {}", e.getMessage());
            throw e; // Re-throw to be handled by caller
        }
    }

    /**
     * Makes a streaming HTTP call to the Anthropic messages API.
     *
     * @param requestPayload the request payload
     * @param baseUrl the base URL of the provider
     * @param apiKey the API key for authentication
     * @return a Flux of response chunks as strings
     * @throws Exception if the HTTP call fails
     */
    public reactor.core.publisher.Flux<String> messagesStream(Map<String, Object> requestPayload, String baseUrl, String apiKey) {
        return null;
    }
}