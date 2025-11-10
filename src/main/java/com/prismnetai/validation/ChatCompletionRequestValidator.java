package com.prismnetai.validation;

import java.util.HashMap;
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
        validateRoutingStrategy(request, errors);
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
     * Validates the routing strategy field.
     */
    private void validateRoutingStrategy(ChatCompletionRequest request, Map<String, String> errors) {
        if (!StringUtils.hasText(request.getRoutingStrategy())) {
            errors.put("routingStrategy", "routingStrategy cannot be null or empty");
            return;
        }

        // Validate against known routing strategies
        try {
            com.prismnetai.entity.AiRequest.RoutingStrategy.valueOf(request.getRoutingStrategy().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.put("routingStrategy", "routingStrategy must be one of: PRICE, LATENCY, THROUGHPUT, AUTO, CUSTOM_ORDER");
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