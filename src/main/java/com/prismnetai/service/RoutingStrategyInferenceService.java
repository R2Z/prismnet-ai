package com.prismnetai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.entity.AiRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for inferring routing strategy from flexible ChatCompletionRequest structure.
 * Maps new request formats to appropriate routing strategies.
 */
@Slf4j
@Service
public class RoutingStrategyInferenceService {

    /**
     * Infers the routing strategy and preferred model from a ChatCompletionRequest.
     *
     * @param request the chat completion request
     * @return RoutingInferenceResult containing the inferred strategy and model
     */
    public RoutingInferenceResult inferRoutingStrategy(ChatCompletionRequest request) {
        log.info("RoutingStrategyInferenceService.inferRoutingStrategy() - Inferring routing strategy from request");

        // Infer from new flexible fields
        return inferFromFlexibleFields(request);
    }

    /**
     * Extracts preferred model based on routing strategy.
     */
    private String extractPreferredModel(ChatCompletionRequest request, AiRequest.RoutingStrategy strategy) {
        // For preferred model strategy, use the specified model
        if (strategy == AiRequest.RoutingStrategy.PREFERRED_MODEL && StringUtils.hasText(request.getModel())) {
            return request.getModel();
        }
        // For other strategies, use the model field if specified
        if (StringUtils.hasText(request.getModel())) {
            return request.getModel();
        }
        return null; // no fallback
    }

    /**
     * Infers routing strategy from the new flexible request fields.
     */
    private RoutingInferenceResult inferFromFlexibleFields(ChatCompletionRequest request) {
        boolean hasModel = StringUtils.hasText(request.getModel());
        boolean hasModels = request.getModels() != null && !request.getModels().isEmpty();
        boolean hasProvider = request.getProvider() != null;

        // Case 1: Multiple models specified - use CUSTOM_ORDER with fallback
        if (hasModels) {
            log.info("RoutingStrategyInferenceService.inferFromFlexibleFields() - Multiple models specified, using CUSTOM_ORDER strategy");
            return new RoutingInferenceResult(AiRequest.RoutingStrategy.CUSTOM_ORDER, null, request.getProvider());
        }

        // Case 2: Single model with provider options
        if (hasModel && hasProvider) {
            AiRequest.RoutingStrategy strategy = inferStrategyFromProviderOptions(request.getProvider());
            log.info("RoutingStrategyInferenceService.inferFromFlexibleFields() - Single model with provider options, using strategy: {}", strategy);
            return new RoutingInferenceResult(strategy, request.getModel(), request.getProvider());
        }

        // Case 3: Single model only - use PREFERRED_MODEL strategy
        if (hasModel) {
            log.info("RoutingStrategyInferenceService.inferFromFlexibleFields() - Single model specified, using PREFERRED_MODEL strategy");
            return new RoutingInferenceResult(AiRequest.RoutingStrategy.PREFERRED_MODEL, request.getModel(), request.getProvider());
        }

        // Case 4: Provider options only - infer from provider configuration
        if (hasProvider) {
            AiRequest.RoutingStrategy strategy = inferStrategyFromProviderOptions(request.getProvider());
            log.info("RoutingStrategyInferenceService.inferFromFlexibleFields() - Provider options only, using strategy: {}", strategy);
            return new RoutingInferenceResult(strategy, null, request.getProvider());
        }

        // Case 5: No routing configuration - default to AUTO
        log.info("RoutingStrategyInferenceService.inferFromFlexibleFields() - No routing configuration, defaulting to AUTO strategy");
        return new RoutingInferenceResult(AiRequest.RoutingStrategy.AUTO, null, null);
    }

    /**
     * Infers routing strategy from provider options.
     */
    private AiRequest.RoutingStrategy inferStrategyFromProviderOptions(ChatCompletionRequest.ProviderOptions provider) {
        // If custom order is specified, use CUSTOM_ORDER
        if (provider.getOrder() != null && !provider.getOrder().isEmpty()) {
            return AiRequest.RoutingStrategy.CUSTOM_ORDER;
        }

        // If sort criteria is specified, map to corresponding strategy
        if (StringUtils.hasText(provider.getSort())) {
            String sort = provider.getSort().toLowerCase();
            switch (sort) {
                case "throughput":
                    return AiRequest.RoutingStrategy.THROUGHPUT;
                case "latency":
                    return AiRequest.RoutingStrategy.LATENCY;
                case "price":
                case "cost":
                    return AiRequest.RoutingStrategy.PRICE;
                default:
                    log.warn("RoutingStrategyInferenceService.inferStrategyFromProviderOptions() - Unknown sort criteria: {}, defaulting to AUTO", sort);
                    return AiRequest.RoutingStrategy.AUTO;
            }
        }

        // Default to AUTO if no specific criteria
        return AiRequest.RoutingStrategy.AUTO;
    }

    /**
     * Result class containing inferred routing information.
     */
    public static class RoutingInferenceResult {
        private final AiRequest.RoutingStrategy strategy;
        private final String preferredModel;
        private final ChatCompletionRequest.ProviderOptions providerOptions;

        public RoutingInferenceResult(AiRequest.RoutingStrategy strategy, String preferredModel, ChatCompletionRequest.ProviderOptions providerOptions) {
            this.strategy = strategy;
            this.preferredModel = preferredModel;
            this.providerOptions = providerOptions;
        }

        public AiRequest.RoutingStrategy getStrategy() {
            return strategy;
        }

        public String getPreferredModel() {
            return preferredModel;
        }

        public ChatCompletionRequest.ProviderOptions getProviderOptions() {
            return providerOptions;
        }

        public List<String> getModelFallbacks() {
            return providerOptions != null && providerOptions.getOrder() != null ? providerOptions.getOrder() : null;
        }

        public Boolean allowFallbacks() {
            return providerOptions != null ? providerOptions.getAllowFallbacks() : null;
        }
    }
}