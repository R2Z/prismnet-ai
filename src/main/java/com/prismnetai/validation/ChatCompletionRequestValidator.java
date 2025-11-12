package com.prismnetai.validation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.exception.ValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Validator for ChatCompletionRequest objects. This class provides comprehensive
 * validation for chat completion requests with detailed error messages.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class ChatCompletionRequestValidator {

    /**
     * Validates a chat completion request.
     *
     * @param request the request to validate
     * @throws ValidationException if validation fails with detailed field-level error information
     */
    public void validate(ChatCompletionRequest request) {
        log.info("ChatCompletionRequestValidator.validate() - Validating chat completion request");

        Map<String, String> errors = new HashMap<>();

        validateMessages(request, errors);
        validateRoutingConfiguration(request, errors);
        validateMaxTokens(request, errors);
        validateTemperature(request, errors);

        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed for " + errors.size() + " field(s)";
            log.warn("ChatCompletionRequestValidator.validate() - Validation failed: {}", errorMessage);
            throw new ValidationException(errorMessage, errors);
        }

        log.info("ChatCompletionRequestValidator.validate() - Validation successful");
    }

    /**
     * Validates the messages field.
     */
    private void validateMessages(ChatCompletionRequest request, Map<String, String> errors) {
        if (request.getMessages() == null) {
            errors.put("messages", "messages cannot be null");
            return;
        }

        if (request.getMessages().isEmpty()) {
            errors.put("messages", "messages cannot be empty");
            return;
        }

        // Check for at least one user message
        boolean hasUserMessage = request.getMessages().stream()
            .anyMatch(msg -> "user".equals(msg.getRole()));

        if (!hasUserMessage) {
            errors.put("messages", "messages must contain at least one user message");
        }

        // Validate individual messages
        for (int i = 0; i < request.getMessages().size(); i++) {
            var message = request.getMessages().get(i);
            if (message.getRole() == null || message.getRole().trim().isEmpty()) {
                errors.put("messages", "message[" + i + "].role cannot be null or empty");
            }
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                errors.put("messages", "message[" + i + "].content cannot be null or empty");
            }
        }
    }

    /**
     * Validates the routing configuration fields.
     * Supports new flexible routing fields.
     */
    private void validateRoutingConfiguration(ChatCompletionRequest request, Map<String, String> errors) {
        // Check if any routing configuration is provided
        boolean hasModel = StringUtils.hasText(request.getModel());
        boolean hasModels = request.getModels() != null && !request.getModels().isEmpty();
        boolean hasProvider = request.getProvider() != null;

        // At least one routing configuration must be present
        if (!hasModel && !hasModels && !hasProvider) {
            errors.put("routing", "At least one routing configuration must be provided: model, models, or provider");
            return;
        }

        // Validate single model field
        if (hasModel) {
            if (request.getModel().trim().isEmpty()) {
                errors.put("model", "model cannot be empty if provided");
            }
            // Check for conflicting configurations
            if (hasModels) {
                errors.put("routing", "Cannot specify both 'model' and 'models' fields");
            }
        }

        // Validate models list
        if (hasModels) {
            for (int i = 0; i < request.getModels().size(); i++) {
                String model = request.getModels().get(i);
                if (!StringUtils.hasText(model)) {
                    errors.put("models", "models[" + i + "] cannot be null or empty");
                }
            }
        }

        // Validate provider options
        if (hasProvider) {
            validateProviderOptions(request.getProvider(), errors);
        }
    }

    /**
     * Validates provider options configuration.
     */
    private void validateProviderOptions(ChatCompletionRequest.ProviderOptions provider, Map<String, String> errors) {
        // Validate sort field
        if (StringUtils.hasText(provider.getSort())) {
            String sort = provider.getSort().toLowerCase();
            List<String> validSorts = Arrays.asList("throughput", "latency", "price", "cost");
            if (!validSorts.contains(sort)) {
                errors.put("provider.sort", "provider.sort must be one of: throughput, latency, price, cost");
            }
        }

        // Validate order list
        if (provider.getOrder() != null && !provider.getOrder().isEmpty()) {
            for (int i = 0; i < provider.getOrder().size(); i++) {
                String providerName = provider.getOrder().get(i);
                if (!StringUtils.hasText(providerName)) {
                    errors.put("provider.order", "provider.order[" + i + "] cannot be null or empty");
                }
            }
        }
    }

    /**
     * Validates the maxTokens field.
     */
    private void validateMaxTokens(ChatCompletionRequest request, Map<String, String> errors) {
        if (request.getMaxTokens() != null) {
            if (request.getMaxTokens() <= 0) {
                errors.put("maxTokens", "maxTokens must be positive");
            } else if (request.getMaxTokens() > 32768) { // Reasonable upper limit
                errors.put("maxTokens", "maxTokens cannot exceed 32768");
            }
        }
    }

    /**
     * Validates the temperature field.
     */
    private void validateTemperature(ChatCompletionRequest request, Map<String, String> errors) {
        if (request.getTemperature() != null) {
            double temp = request.getTemperature().doubleValue();
            if (temp < 0.0 || temp > 2.0) {
                errors.put("temperature", "temperature must be between 0.0 and 2.0");
            }
        }
    }
}