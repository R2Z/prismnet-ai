package com.prismnetai.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prismnetai.entity.AiRequest;
import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.repository.AiRequestRepository;
import com.prismnetai.repository.ProviderRepository;
import com.prismnetai.service.routing.RoutingStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final ProviderRepository providerRepository;
    private final AiRequestRepository aiRequestRepository;
    private final Map<String, RoutingStrategy> routingStrategies;

    /**
     * Routes an AI request based on the specified routing strategy.
     * This method validates inputs, selects an appropriate model using the routing strategy,
     * and creates a persistent record of the request.
     *
     * @param userId the ID of the user making the request
     * @param routingStrategy the routing strategy to use for model selection
     * @param prompt the text prompt for the AI request
     * @param maxTokens the maximum number of tokens to generate (can be null)
     * @param preferredModel the preferred model ID (can be null)
     * @return the created AiRequest entity with routing information
     * @throws RoutingException if routing fails due to no available providers or models
     * @throws IllegalArgumentException if input parameters are invalid
     */
    @Transactional
    public AiRequest routeRequest(String userId, AiRequest.RoutingStrategy routingStrategy,
                                     String prompt, Integer maxTokens, String preferredModel) {
        return routeRequest(userId, routingStrategy, prompt, maxTokens, preferredModel, null);
    }

    /**
     * Routes an AI request based on inferred routing strategy from flexible request format.
     * This method validates inputs, selects an appropriate model using the routing strategy,
     * and creates a persistent record of the request.
     *
     * @param userId the ID of the user making the request
     * @param inferenceResult the inferred routing strategy and configuration
     * @param prompt the text prompt for the AI request
     * @param maxTokens the maximum number of tokens to generate (can be null)
     * @return the created AiRequest entity with routing information
     * @throws RoutingException if routing fails due to no available providers or models
     * @throws IllegalArgumentException if input parameters are invalid
     */
    @Transactional
    public AiRequest routeRequest(String userId, RoutingStrategyInferenceService.RoutingInferenceResult inferenceResult,
                                     String prompt, Integer maxTokens) {
        return routeRequest(userId, inferenceResult.getStrategy(), prompt, maxTokens,
                           inferenceResult.getPreferredModel(), inferenceResult.getProviderOptions());
    }

    /**
     * Internal routing method that handles both legacy and new routing approaches.
     */
    private AiRequest routeRequest(String userId, AiRequest.RoutingStrategy routingStrategy,
                                     String prompt, Integer maxTokens, String preferredModel,
                                     com.prismnetai.dto.ChatCompletionRequest.ProviderOptions providerOptions) {

        // Input validation
        validateRouteRequestInputs(userId, routingStrategy, prompt);

        log.info("RoutingService.routeRequest() - Starting request routing for userId: {}, strategy: {}, promptLength: {}, maxTokens: {}",
                  userId, routingStrategy, prompt.length(), maxTokens);

        // Get available providers
        List<Provider> availableProviders = getAvailableProvidersInternal();
        if (availableProviders.isEmpty()) {
            log.error("RoutingService.routeRequest() - No active providers available for routing");
            throw new com.prismnetai.exception.RoutingException("No active providers available for routing");
        }

        // Select and validate routing strategy
        RoutingStrategy strategy = getRoutingStrategy(routingStrategy);

        // Select model using strategy
        Optional<Model> selectedModel = strategy.selectModel(availableProviders, userId, preferredModel);
        if (selectedModel.isEmpty()) {
            log.error("RoutingService.routeRequest() - No suitable model found for strategy: {} among {} providers",
                        routingStrategy, availableProviders.size());
            throw new com.prismnetai.exception.RoutingException("No suitable model found for routing strategy: " + routingStrategy);
        }

        // TODO: Apply provider-specific filtering and ordering if providerOptions provided
        // This will be implemented when we enhance the routing strategies to support provider options

        Model model = selectedModel.get();
        log.info("RoutingService.routeRequest() - Selected model: {} from provider: {} for user: {}",
                  model.getModelId(), model.getProvider().getName(), userId);

        // Create and save request record
        AiRequest request = createAiRequest(userId, routingStrategy, prompt, maxTokens, model);
        AiRequest savedRequest = aiRequestRepository.save(request);

        log.info("RoutingService.routeRequest() - Successfully created request with ID: {} for user: {}",
                  savedRequest.getId(), userId);

        return savedRequest;
    }

    /**
     * Validates the input parameters for routeRequest method.
     */
    private void validateRouteRequestInputs(String userId, AiRequest.RoutingStrategy routingStrategy, String prompt) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (routingStrategy == null) {
            throw new IllegalArgumentException("Routing strategy cannot be null");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
    }

    /**
     * Retrieves available active providers.
     */
    private List<Provider> getAvailableProvidersInternal() {
        List<Provider> providers = providerRepository.findByIsActiveTrue();
        log.info("RoutingService.getAvailableProvidersInternal() - Found {} active providers", providers.size());
        return providers;
    }

    /**
     * Retrieves and validates the routing strategy.
     */
    private RoutingStrategy getRoutingStrategy(AiRequest.RoutingStrategy routingStrategy) {
        RoutingStrategy strategy = routingStrategies.get(routingStrategy.name());
        if (strategy == null) {
            log.error("RoutingService.getRoutingStrategy() - Unknown routing strategy requested: {}", routingStrategy);
            throw new IllegalArgumentException("Unknown routing strategy: " + routingStrategy);
        }
        log.info("RoutingService.getRoutingStrategy() - Using routing strategy: {}", strategy.getStrategyName());
        return strategy;
    }

    /**
     * Creates an AiRequest entity with the provided parameters.
     */
    private AiRequest createAiRequest(String userId, AiRequest.RoutingStrategy routingStrategy,
                                    String prompt, Integer maxTokens, Model model) {
        AiRequest request = new AiRequest();
        request.setUserId(userId);
        request.setRoutingStrategy(routingStrategy);
        request.setPrompt(prompt);
        request.setMaxTokens(maxTokens);
        request.setSelectedProvider(model.getProvider());
        request.setSelectedModel(model);
        request.setStatus(AiRequest.RequestStatus.PENDING);
        return request;
    }

    /**
     * Retrieves all available active providers.
     * This method returns an immutable view of the providers list.
     *
     * @return list of active providers (unmodifiable)
     */
    public List<Provider> getAvailableProviders() {
        log.info("RoutingService.getAvailableProviders() - Retrieving active providers");
        List<Provider> providers = providerRepository.findByIsActiveTrue();
        log.info("RoutingService.getAvailableProviders() - Found {} active providers", providers.size());
        return List.copyOf(providers); // Return immutable copy
    }

    /**
     * Retrieves all AI requests for a specific user, ordered by creation date (newest first).
     * This method returns an immutable view of the requests list.
     *
     * @param userId the ID of the user whose requests to retrieve
     * @return list of user's AI requests (unmodifiable), ordered by creation date descending
     * @throws IllegalArgumentException if userId is null or empty
     */
    public List<AiRequest> getUserRequests(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        log.info("RoutingService.getUserRequests() - Retrieving requests for userId: {}", userId);
        List<AiRequest> requests = aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.info("RoutingService.getUserRequests() - Found {} requests for user: {}", requests.size(), userId);
        return List.copyOf(requests); // Return immutable copy
    }
}