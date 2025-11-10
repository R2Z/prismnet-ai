package com.prismnetai.service.routing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${prismnet.routing.default-user-id:default}")
    private String defaultUserId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders, String userId) {
        log.info("CustomOrderRoutingStrategy.selectModel() - Starting custom order-based model selection");

        if (availableProviders == null) {
            log.warn("CustomOrderRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        if (availableProviders.isEmpty()) {
            log.warn("CustomOrderRoutingStrategy.selectModel() - Empty providers list provided");
            return Optional.empty();
        }

        log.info("CustomOrderRoutingStrategy.selectModel() - Evaluating {} available providers: {}",
                  availableProviders.size(),
                  availableProviders.stream().map(Provider::getName).toList());

        // Determine the effective user ID: use provided userId if not null, otherwise use default
        String effectiveUserId = userId != null ? userId : defaultUserId;
        log.info("CustomOrderRoutingStrategy.selectModel() - Using userId: {} (provided: {}, default: {})",
                  effectiveUserId, userId, defaultUserId);

        // Get active routing rules for the effective user
        List<RoutingRule> activeRules = routingRuleRepository.findActiveRulesByUserIdOrderedById(effectiveUserId);

        if (activeRules.isEmpty()) {
            log.warn("CustomOrderRoutingStrategy.selectModel() - No active routing rules found for user: {}", effectiveUserId);
            return Optional.empty();
        }

        // Create a map of provider name to provider for quick lookup (shared across rules)
        Map<String, Provider> providerByName = availableProviders.stream()
                .collect(Collectors.toMap(Provider::getName, p -> p));

        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();

        // Get active models (shared across rules)
        List<Model> activeModels = modelRepository.findActiveModelsByProviderIds(providerIds);

        // Iterate over active rules to find a suitable model
        for (RoutingRule rule : activeRules) {
            log.info("CustomOrderRoutingStrategy.selectModel() - Evaluating routing rule: {} with provider order: {}",
                      rule.getName(), rule.getProviderOrder());

            // Parse provider order from the rule
            List<String> providerOrder;
            try {
                providerOrder = objectMapper.readValue(rule.getProviderOrder(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                log.error("CustomOrderRoutingStrategy.selectModel() - Failed to parse provider order JSON for rule: {}", rule.getName(), e);
                continue;
            }

            // Create a map of provider ID to priority order (lower number = higher priority)
            Map<Long, Integer> providerPriority = providerOrder.stream()
                    .filter(providerByName::containsKey)
                    .collect(Collectors.toMap(
                        name -> providerByName.get(name).getId(),
                        name -> providerOrder.indexOf(name)
                    ));

            Optional<Model> selectedModel = activeModels.stream()
                    .filter(model -> {
                        boolean isAvailable = providerIds.contains(model.getProvider().getId());
                        boolean hasPriority = providerPriority.containsKey(model.getProvider().getId());
                        log.info("CustomOrderRoutingStrategy.selectModel() - Model {} from provider {} is available: {}, has priority: {}",
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
                return selectedModel;
            }
        }

        log.warn("CustomOrderRoutingStrategy.selectModel() - No suitable model found among available providers matching any custom order rule");
        return Optional.empty();
    }

    @Override
    public String getStrategyName() {
        return "CUSTOM_ORDER";
    }
}