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

    @Transactional
    public AiRequest routeRequest(String userId, AiRequest.RoutingStrategy routingStrategy,
                                  String prompt, Integer maxTokens) {

        log.info("RoutingService.routeRequest() - Starting request routing for userId: {}, strategy: {}, promptLength: {}, maxTokens: {}",
                 userId, routingStrategy, prompt.length(), maxTokens);

        // Get available providers
        List<Provider> availableProviders = providerRepository.findByIsActiveTrue();
        log.debug("RoutingService.routeRequest() - Found {} active providers", availableProviders.size());

        if (availableProviders.isEmpty()) {
            log.error("RoutingService.routeRequest() - No active providers available for routing");
            throw new RuntimeException("No active providers available");
        }

        // Select routing strategy
        RoutingStrategy strategy = routingStrategies.get(routingStrategy.name());
        if (strategy == null) {
            log.error("RoutingService.routeRequest() - Unknown routing strategy requested: {}", routingStrategy);
            throw new IllegalArgumentException("Unknown routing strategy: " + routingStrategy);
        }

        log.debug("RoutingService.routeRequest() - Using routing strategy: {}", strategy.getStrategyName());

        // Select model using strategy
        Optional<Model> selectedModel = strategy.selectModel(availableProviders, userId);
        if (selectedModel.isEmpty()) {
            log.error("RoutingService.routeRequest() - No suitable model found for strategy: {} among {} providers",
                      routingStrategy, availableProviders.size());
            throw new RuntimeException("No suitable model found for routing strategy: " + routingStrategy);
        }

        Model model = selectedModel.get();
        log.info("RoutingService.routeRequest() - Selected model: {} from provider: {} for user: {}",
                 model.getModelId(), model.getProvider().getName(), userId);

        // Create request record
        AiRequest request = new AiRequest();
        request.setUserId(userId);
        request.setRoutingStrategy(routingStrategy);
        request.setPrompt(prompt);
        request.setMaxTokens(maxTokens);
        request.setSelectedProvider(model.getProvider());
        request.setSelectedModel(model);
        request.setStatus(AiRequest.RequestStatus.PENDING);

        AiRequest savedRequest = aiRequestRepository.save(request);
        log.info("RoutingService.routeRequest() - Successfully created request with ID: {} for user: {}",
                 savedRequest.getId(), userId);

        return savedRequest;
    }

    public List<Provider> getAvailableProviders() {
        log.debug("RoutingService.getAvailableProviders() - Retrieving active providers");
        List<Provider> providers = providerRepository.findByIsActiveTrue();
        log.info("RoutingService.getAvailableProviders() - Found {} active providers", providers.size());
        return providers;
    }

    public List<AiRequest> getUserRequests(String userId) {
        log.debug("RoutingService.getUserRequests() - Retrieving requests for userId: {}", userId);
        List<AiRequest> requests = aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.info("RoutingService.getUserRequests() - Found {} requests for user: {}", requests.size(), userId);
        return requests;
    }
}