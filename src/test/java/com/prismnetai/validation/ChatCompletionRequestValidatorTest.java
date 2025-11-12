package com.prismnetai.validation;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.exception.ValidationException;

@ExtendWith(MockitoExtension.class)
public class ChatCompletionRequestValidatorTest {

    private final ChatCompletionRequestValidator validator = new ChatCompletionRequestValidator();

    @Test
    void testValidSingleModelRequest() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4o")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void testValidMultipleModelsRequest() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .models(Arrays.asList("anthropic/claude-3.5-sonnet", "gryphe/mythomax-l2-13b"))
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void testValidProviderOptionsRequest() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .sort("throughput")
                .order(Arrays.asList("openai", "together"))
                .allowFallbacks(false)
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void testValidLegacyRoutingStrategyRequest() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .routingStrategy("PRICE")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void testInvalidNoRoutingConfiguration() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("routing"));
    }

    @Test
    void testInvalidEmptyModel() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("") // empty string
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        // The validation should fail because empty model is not considered valid routing config
        // So it should fail with "routing" error, not "model" error
        assertTrue(exception.getValidationErrors().containsKey("routing"));
    }

    @Test
    void testInvalidConflictingModelAndModels() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .models(Arrays.asList("anthropic/claude-3", "gryphe/model"))
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("routing"));
    }

    @Test
    void testInvalidEmptyModelsList() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .models(Arrays.asList(""))
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("models"));
    }

    @Test
    void testInvalidProviderSort() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .sort("invalid_sort")
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("provider.sort"));
    }

    @Test
    void testInvalidProviderOrder() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .order(Arrays.asList("openai", ""))
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("provider.order"));
    }

    @Test
    void testInvalidLegacyRoutingStrategy() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .routingStrategy("INVALID_STRATEGY")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("routingStrategy"));
    }

    @Test
    void testInvalidNoMessages() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("messages"));
    }

    @Test
    void testInvalidEmptyMessages() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of())
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("messages"));
    }

    @Test
    void testInvalidNoUserMessage() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("system", "You are helpful")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("messages"));
    }

    @Test
    void testInvalidEmptyMessageRole() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("", "Hello")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("messages"));
    }

    @Test
    void testInvalidEmptyMessageContent() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "")))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("messages"));
    }

    @Test
    void testValidTemperatureRange() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .temperature(BigDecimal.valueOf(1.5))
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void testInvalidTemperatureTooLow() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .temperature(BigDecimal.valueOf(-0.1))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("temperature"));
    }

    @Test
    void testInvalidTemperatureTooHigh() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .temperature(BigDecimal.valueOf(2.1))
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("temperature"));
    }

    @Test
    void testInvalidMaxTokensNegative() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .maxTokens(-1)
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("maxTokens"));
    }

    @Test
    void testInvalidMaxTokensTooHigh() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .maxTokens(40000)
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getValidationErrors().containsKey("maxTokens"));
    }
}