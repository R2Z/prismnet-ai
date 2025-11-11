package com.prismnetai.service.provider.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client service for handling OpenAI API HTTP calls.
 * This class encapsulates the HTTP communication logic specific to OpenAI API.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiApiClient {

    private static final String COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    /**
     * Makes an HTTP call to the OpenAI chat completions API.
     *
     * @param requestPayload the request payload
     * @param baseUrl the base URL of the provider
     * @param apiKey the API key for authentication
     * @return the response body as a string
     * @throws Exception if the HTTP call fails
     */
    public String chatCompletions(Map<String, Object> requestPayload, String baseUrl, String apiKey) {
        try {
            String fullUrl = baseUrl + COMPLETIONS_ENDPOINT;

            log.info("OpenAiApiClient.callApi() - Headers: Content-Type={}, Authorization=Bearer [REDACTED]", MediaType.APPLICATION_JSON_VALUE);

            return webClient.post()
                .uri(fullUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(API_TIMEOUT)
                .block();
        } catch (Exception e) {
            log.error("OpenAiApiClient.callApi() - Failed to make API call to OpenAI: {}", e.getMessage());
            throw e; // Re-throw to be handled by caller
        }
    }
}