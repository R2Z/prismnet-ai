package com.prismnetai.validation;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatCompletionRequestValidator.
 * Tests validation logic for chat completion requests.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@DisplayName("ChatCompletionRequestValidator Tests")
class ChatCompletionRequestValidatorTest {

    private ChatCompletionRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChatCompletionRequestValidator();
    }

    @Test
    @DisplayName("Should validate valid request successfully")
    void shouldValidateValidRequest() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("AUTO");
        request.setMessages(List.of(
            new ChatCompletionRequest.ChatMessage("user", "Hello, world!")
        ));
        request.setMaxTokens(100);
        request.setTemperature(BigDecimal.valueOf(0.7));

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should throw ValidationException for null messages")
    void shouldThrowExceptionForNullMessages() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("AUTO");
        request.setMessages(null);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("messages"));
        assertEquals("messages cannot be null", exception.getValidationErrors().get("messages"));
    }

    @Test
    @DisplayName("Should throw ValidationException for empty messages")
    void shouldThrowExceptionForEmptyMessages() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("AUTO")
            .messages(List.of())
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("messages"));
        assertEquals("messages cannot be empty", exception.getValidationErrors().get("messages"));
    }

    @Test
    @DisplayName("Should throw ValidationException for missing user message")
    void shouldThrowExceptionForMissingUserMessage() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("AUTO")
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("system")
                    .content("You are a helpful assistant")
                    .build()
            ))
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("messages"));
        assertEquals("messages must contain at least one user message", exception.getValidationErrors().get("messages"));
    }

    @Test
    @DisplayName("Should throw ValidationException for null routing strategy")
    void shouldThrowExceptionForNullRoutingStrategy() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy(null)
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("user")
                    .content("Hello!")
                    .build()
            ))
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("routingStrategy"));
        assertEquals("routingStrategy cannot be null or empty", exception.getValidationErrors().get("routingStrategy"));
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid routing strategy")
    void shouldThrowExceptionForInvalidRoutingStrategy() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("INVALID_STRATEGY")
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("user")
                    .content("Hello!")
                    .build()
            ))
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("routingStrategy"));
        assertEquals("routingStrategy must be one of: PRICE, LATENCY, THROUGHPUT, AUTO, CUSTOM_ORDER",
            exception.getValidationErrors().get("routingStrategy"));
    }

    @Test
    @DisplayName("Should throw ValidationException for negative maxTokens")
    void shouldThrowExceptionForNegativeMaxTokens() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("AUTO")
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("user")
                    .content("Hello!")
                    .build()
            ))
            .maxTokens(-1)
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("maxTokens"));
        assertEquals("maxTokens must be positive", exception.getValidationErrors().get("maxTokens"));
    }

    @Test
    @DisplayName("Should throw ValidationException for maxTokens exceeding limit")
    void shouldThrowExceptionForMaxTokensExceedingLimit() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("AUTO")
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("user")
                    .content("Hello!")
                    .build()
            ))
            .maxTokens(40000)
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("maxTokens"));
        assertEquals("maxTokens cannot exceed 32768", exception.getValidationErrors().get("maxTokens"));
    }

    @Test
    @DisplayName("Should throw ValidationException for temperature below minimum")
    void shouldThrowExceptionForTemperatureBelowMinimum() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("AUTO")
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("user")
                    .content("Hello!")
                    .build()
            ))
            .temperature(BigDecimal.valueOf(-0.1))
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("temperature"));
        assertEquals("temperature must be between 0.0 and 2.0", exception.getValidationErrors().get("temperature"));
    }

    @Test
    @DisplayName("Should throw ValidationException for temperature above maximum")
    void shouldThrowExceptionForTemperatureAboveMaximum() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("AUTO")
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("user")
                    .content("Hello!")
                    .build()
            ))
            .temperature(BigDecimal.valueOf(2.5))
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertTrue(exception.getValidationErrors().containsKey("temperature"));
        assertEquals("temperature must be between 0.0 and 2.0", exception.getValidationErrors().get("temperature"));
    }

    @Test
    @DisplayName("Should validate request with multiple validation errors")
    void shouldValidateRequestWithMultipleErrors() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .routingStrategy("INVALID")
            .messages(List.of(
                ChatCompletionRequest.ChatMessage.builder()
                    .role("system")
                    .content("")
                    .build()
            ))
            .maxTokens(-5)
            .temperature(BigDecimal.valueOf(3.0))
            .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> validator.validate(request));

        assertEquals(4, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().containsKey("routingStrategy"));
        assertTrue(exception.getValidationErrors().containsKey("messages"));
        assertTrue(exception.getValidationErrors().containsKey("maxTokens"));
        assertTrue(exception.getValidationErrors().containsKey("temperature"));
    }
}