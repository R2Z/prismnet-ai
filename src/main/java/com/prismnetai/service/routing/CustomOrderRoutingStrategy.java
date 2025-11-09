package com.prismnetai.service.routing;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.entity.RoutingRule;
import com.prismnetai.repository.ModelRepository;
import com.prismnetai.repository.RoutingRuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("CUSTOM_ORDER")
@RequiredArgsConstructor
public class CustomOrderRoutingStrategy implements RoutingStrategy {

    private final ModelRepository modelRepository;
    private final RoutingRuleRepository routingRuleRepository;

    // For simplicity, using a default user ID. In a real implementation,
    // this would be passed from the request context
    private static final String DEFAULT_USER_ID = "default";

    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders) {
        log.info("CustomOrderRoutingStrategy.selectModel() - Starting custom order-based model selection");

        if (availableProviders == null) {
            log.warn("CustomOrderRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        if (availableProviders.isEmpty()) {
            log.warn("CustomOrderRoutingStrategy.selectModel() - Empty providers list provided");
            return Optional.empty();
        }

        log.debug("CustomOrderRoutingStrategy.selectModel() - Evaluating {} available providers: {}",
                  availableProviders.size(),
                  availableProviders.stream().map(Provider::getName).toList());

        // Get active routing rules for the default user
        List<RoutingRule> activeRules = routingRuleRepository.findActiveRulesByUserIdOrderedById(DEFAULT_USER_ID);

        if (activeRules.isEmpty()) {
            log.warn("CustomOrderRoutingStrategy.selectModel() - No active routing rules found for user: {}", DEFAULT_USER_ID);
            return Optional.empty();
        }

        // Use the first active rule (in a real implementation, you might want to select based on some criteria)
        RoutingRule rule = activeRules.get(0);
        log.debug("CustomOrderRoutingStrategy.selectModel() - Using routing rule: {} with provider order: {}",
                  rule.getName(), rule.getProviderOrder());

        // Parse provider order from the rule
        List<String> providerOrder = Arrays.asList(rule.getProviderOrder().split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());

        // Create a map of provider name to provider for quick lookup
        Map<String, Provider> providerByName = availableProviders.stream()
                .collect(Collectors.toMap(Provider::getName, p -> p));

        // Create a map of provider ID to priority order (lower number = higher priority)
        Map<Long, Integer> providerPriority = providerOrder.stream()
                .filter(providerByName::containsKey)
                .collect(Collectors.toMap(
                    name -> providerByName.get(name).getId(),
                    name -> providerOrder.indexOf(name)
                ));

        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();

        // Get active models and sort by custom order
        List<Model> activeModels = modelRepository.findActiveModelsByProviderIds(providerIds);

        Optional<Model> selectedModel = activeModels.stream()
                .filter(model -> {
                    boolean isAvailable = providerIds.contains(model.getProvider().getId());
                    boolean hasPriority = providerPriority.containsKey(model.getProvider().getId());
                    log.debug("CustomOrderRoutingStrategy.selectModel() - Model {} from provider {} is available: {}, has priority: {}",
                              model.getModelId(), model.getProvider().getName(), isAvailable, hasPriority);
                    return isAvailable && hasPriority;
                })
                .sorted((a, b) -> {
                    Integer priorityA = providerPriority.get(a.getProvider().getId());
                    Integer priorityB = providerPriority.get(b.getProvider().getId());
                    // Sort by priority ascending (lower priority number first)
                    return priorityA.compareTo(priorityB);
                })
                .findFirst();

        if (selectedModel.isPresent()) {
            Model model = selectedModel.get();
            log.info("CustomOrderRoutingStrategy.selectModel() - Selected model: {} from provider {} based on custom order rule: {}",
                     model.getModelId(), model.getProvider().getName(), rule.getName());
        } else {
            log.warn("CustomOrderRoutingStrategy.selectModel() - No suitable model found among available providers matching custom order");
        }

        log.info("CustomOrderRoutingStrategy.selectModel() - Completed custom order-based model selection");
        return selectedModel;
    }

    @Override
    public String getStrategyName() {
        return "CUSTOM_ORDER";
    }
}