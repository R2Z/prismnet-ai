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
@Component("PRICE")
@RequiredArgsConstructor
public class PriceRoutingStrategy implements RoutingStrategy {

    private final ModelRepository modelRepository;

    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders) {
        log.info("PriceRoutingStrategy.selectModel() - Starting price-based model selection");

        if (availableProviders == null) {
            log.warn("PriceRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        if (availableProviders.isEmpty()) {
            log.warn("PriceRoutingStrategy.selectModel() - Empty providers list provided");
            return Optional.empty();
        }

        log.debug("PriceRoutingStrategy.selectModel() - Evaluating {} available providers: {}",
                  availableProviders.size(),
                  availableProviders.stream().map(Provider::getName).toList());

        List<Model> activeModels = modelRepository.findActiveModelsOrderedByLowestCost();
        log.debug("PriceRoutingStrategy.selectModel() - Found {} active models ordered by cost",
                  activeModels.size());

        // Filter models by available providers
        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();

        Optional<Model> selectedModel = activeModels.stream()
                .filter(model -> {
                    boolean isAvailable = providerIds.contains(model.getProvider().getId());
                    log.debug("PriceRoutingStrategy.selectModel() - Model {} from provider {} is {}",
                              model.getModelId(), model.getProvider().getName(),
                              isAvailable ? "available" : "not available");
                    return isAvailable;
                })
                .findFirst();

        if (selectedModel.isPresent()) {
            Model model = selectedModel.get();
            log.info("PriceRoutingStrategy.selectModel() - Selected cheapest model: {} from provider {} with total cost per token: {}",
                     model.getModelId(), model.getProvider().getName(),
                     model.getInputPricing().add(model.getOutputPricing()));
        } else {
            log.warn("PriceRoutingStrategy.selectModel() - No suitable model found among available providers");
        }

        log.info("PriceRoutingStrategy.selectModel() - Completed price-based model selection");
        return selectedModel;
    }

    @Override
    public String getStrategyName() {
        return "PRICE";
    }
}