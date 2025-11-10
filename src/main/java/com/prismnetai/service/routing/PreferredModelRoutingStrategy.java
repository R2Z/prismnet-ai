package com.prismnetai.service.routing;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.repository.ModelRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("PREFERRED_MODEL")
@RequiredArgsConstructor
public class PreferredModelRoutingStrategy implements RoutingStrategy {

    private final ModelRepository modelRepository;

    /**
     * Selects the first active instance of the user's preferred model from available providers.
     * Checks providers in the order they appear in the available_providers list.
     * If the preferred model is found with multiple providers, selects the first one encountered.
     * If the preferred model is not found among active models, returns empty.
     *
     * @param availableProviders list of available providers to choose from
     * @param userId the user ID (not used in preferred model routing)
     * @param preferredModel the preferred model ID to find
     * @return Optional containing the selected model, empty if preferred model not found or no providers available
     */
    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders, String userId, String preferredModel) {
        log.info("PreferredModelRoutingStrategy.selectModel() - Starting preferred model selection for model: {}", preferredModel);

        // Input validation
        if (availableProviders == null || availableProviders.isEmpty()) {
            log.warn("PreferredModelRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        if (preferredModel == null || preferredModel.trim().isEmpty()) {
            log.warn("PreferredModelRoutingStrategy.selectModel() - No preferred model specified");
            return Optional.empty();
        }

        log.info("PreferredModelRoutingStrategy.selectModel() - Evaluating {} available providers for preferred model: {}",
                 availableProviders.size(), preferredModel);

        // Extract provider IDs for efficient querying
        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();

        // Find active models for the preferred model across available providers
        List<Model> matchingModels = modelRepository.findActiveModelsByModelIdAndProviderIds(preferredModel, providerIds);

        if (matchingModels.isEmpty()) {
            log.warn("PreferredModelRoutingStrategy.selectModel() - Preferred model '{}' not found among active models for available providers",
                     preferredModel);
            return Optional.empty();
        }

        // Select the first matching model (providers are checked in order they appear in availableProviders)
        Model selectedModel = matchingModels.get(0);
        log.info("PreferredModelRoutingStrategy.selectModel() - Selected preferred model: {} from provider: {}",
                 selectedModel.getModelId(), selectedModel.getProvider().getName());

        log.info("PreferredModelRoutingStrategy.selectModel() - Completed preferred model selection");
        return Optional.of(selectedModel);
    }

    @Override
    public String getStrategyName() {
        return "PREFERRED_MODEL";
    }
}