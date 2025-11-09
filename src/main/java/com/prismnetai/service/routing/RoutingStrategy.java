package com.prismnetai.service.routing;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;

import java.util.List;
import java.util.Optional;

public interface RoutingStrategy {

    /**
     * Selects the best model from available providers based on the routing strategy
     * @param availableProviders List of providers that are currently available
     * @return Optional containing the selected model, empty if no suitable model found
     */
    Optional<Model> selectModel(List<Provider> availableProviders);

    /**
     * Returns the name of this routing strategy
     * @return Strategy name (e.g., "PRICE", "LATENCY", etc.)
     */
    String getStrategyName();
}