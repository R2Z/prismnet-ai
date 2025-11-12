package com.prismnetai.service;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.entity.AiRequest;

@ExtendWith(MockitoExtension.class)
public class RoutingStrategyInferenceServiceTest {

    private final RoutingStrategyInferenceService inferenceService = new RoutingStrategyInferenceService();

    @Test
    void testInferFromSingleModel() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("openai/gpt-4o")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.PREFERRED_MODEL, result.getStrategy());
        assertEquals("openai/gpt-4o", result.getPreferredModel());
    }

    @Test
    void testInferFromMultipleModels() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .models(Arrays.asList("anthropic/claude-3.5-sonnet", "gryphe/mythomax-l2-13b"))
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.CUSTOM_ORDER, result.getStrategy());
        assertNull(result.getPreferredModel());
    }

    @Test
    void testInferFromProviderSortThroughput() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .sort("throughput")
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.THROUGHPUT, result.getStrategy());
        assertEquals(providerOptions, result.getProviderOptions());
    }

    @Test
    void testInferFromProviderSortLatency() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .sort("latency")
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.LATENCY, result.getStrategy());
        assertEquals(providerOptions, result.getProviderOptions());
    }

    @Test
    void testInferFromProviderSortPrice() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .sort("price")
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.PRICE, result.getStrategy());
        assertEquals(providerOptions, result.getProviderOptions());
    }

    @Test
    void testInferFromProviderOrder() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .order(Arrays.asList("openai", "together"))
                .allowFallbacks(false)
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.CUSTOM_ORDER, result.getStrategy());
        assertEquals(providerOptions, result.getProviderOptions());
        assertEquals(Arrays.asList("openai", "together"), result.getModelFallbacks());
        assertFalse(result.allowFallbacks());
    }

    @Test
    void testInferFromModelWithProviderOptions() {
        // Given
        ChatCompletionRequest.ProviderOptions providerOptions = ChatCompletionRequest.ProviderOptions.builder()
                .sort("throughput")
                .build();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("meta-llama/llama-3.1-70b-instruct")
                .provider(providerOptions)
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.THROUGHPUT, result.getStrategy());
        assertEquals("meta-llama/llama-3.1-70b-instruct", result.getPreferredModel());
        assertEquals(providerOptions, result.getProviderOptions());
    }

    @Test
    void testInferDefaultAutoStrategy() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.AUTO, result.getStrategy());
        assertNull(result.getPreferredModel());
        assertNull(result.getProviderOptions());
    }

    @Test
    void testLegacyRoutingStrategy() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .routingStrategy("PRICE")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.PRICE, result.getStrategy());
    }

    @Test
    void testLegacyRoutingStrategyWithPreferredModel() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .routingStrategy("PREFERRED_MODEL")
                .preferredModel("openai/gpt-4")
                .messages(List.of(new ChatCompletionRequest.ChatMessage("user", "Hello")))
                .build();

        // When
        var result = inferenceService.inferRoutingStrategy(request);

        // Then
        assertEquals(AiRequest.RoutingStrategy.PREFERRED_MODEL, result.getStrategy());
        assertEquals("openai/gpt-4", result.getPreferredModel());
    }
}