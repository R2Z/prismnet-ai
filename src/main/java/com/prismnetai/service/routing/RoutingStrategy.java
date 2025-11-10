package com.prismnetai.service.routing;

import java.util.List;
import java.util.Optional;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;

public interface RoutingStrategy {

    /**
     * Selects the best model from available providers based on the routing strategy
     * @param availableProviders List of providers that are currently available
     * @param userId The user ID for which to select the model (can be null)
     * @param preferredModel The preferred model ID (can be null for strategies that don't use it)
     * @return Optional containing the selected model, empty if no suitable model found
     */
    Optional<Model> selectModel(List<Provider> availableProviders, String userId, String preferredModel);

    /**
     * Returns the name of this routing strategy
     * @return Strategy name (e.g., "PRICE", "LATENCY", etc.)
     */
    String getStrategyName();
}