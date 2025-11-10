package com.prismnetai.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;
import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.service.RoutingService;

@ExtendWith(MockitoExtension.class)
class ChatCompletionControllerTest {

    @Mock
    private RoutingService routingService;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private ChatCompletionController controller;

    private Authentication authentication;
    private Provider provider;
    private Model model;
    private AiRequest aiRequest;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test-user");

        provider = createProvider(1L, "OpenAI");
        model = createModel(1L, provider, "gpt-4");

        aiRequest = new AiRequest();
        aiRequest.setId(100L);
        aiRequest.setUserId("test-user");
        aiRequest.setRoutingStrategy(AiRequest.RoutingStrategy.PRICE);
        aiRequest.setPrompt("Hello, how are you?");
        aiRequest.setMaxTokens(100);
        aiRequest.setSelectedProvider(provider);
        aiRequest.setSelectedModel(model);
        aiRequest.setStatus(AiRequest.RequestStatus.PENDING);
    }

    @Test
    void shouldReturnSuccessfulResponse_whenValidRequestWithPriceRouting() {
        // Given
        ChatCompletionRequest request = createValidRequest("PRICE");
        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq("Hello, how are you?"), eq(100))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ChatCompletionResponse body = response.getBody();
        assertThat(body.getId()).isEqualTo("chatcmpl-100");
        assertThat(body.getObject()).isEqualTo("chat.completion");
        assertThat(body.getModel()).isEqualTo("gpt-4");

        assertThat(body.getRoutingInfo()).isNotNull();
        assertThat(body.getRoutingInfo().getStrategy()).isEqualTo("PRICE");
        assertThat(body.getRoutingInfo().getProvider()).isEqualTo("OpenAI");

        assertThat(body.getChoices()).hasSize(1);
        assertThat(body.getUsage()).isNotNull();
        assertThat(body.getUsage().getTotalTokens()).isEqualTo(30);
    }

    @Test
    void shouldExtractUserMessageFromMessages_whenMultipleMessagesProvided() {
        // Given
        ChatCompletionRequest.ChatMessage systemMessage = new ChatCompletionRequest.ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent("You are a helpful assistant");

        ChatCompletionRequest.ChatMessage userMessage = new ChatCompletionRequest.ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent("Tell me a joke");

        ChatCompletionRequest.ChatMessage assistantMessage = new ChatCompletionRequest.ChatMessage();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent("Why did the chicken cross the road?");

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(systemMessage, userMessage, assistantMessage));
        request.setMaxTokens(50);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq("Tell me a joke"), eq(50))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleNullMessagesGracefully() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(null);
        request.setMaxTokens(100);

        // When & Then
        assertThatThrownBy(() -> controller.createCompletion(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Messages are required");
    }

    @Test
    void shouldHandleEmptyMessagesList() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of());
        request.setMaxTokens(100);

        // When & Then
        assertThatThrownBy(() -> controller.createCompletion(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Messages are required");
    }

    @Test
    void shouldHandleNullRoutingStrategy() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy(null);
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(100);

        // When & Then
        assertThatThrownBy(() -> controller.createCompletion(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing strategy is required");
    }

    @Test
    void shouldHandleEmptyRoutingStrategy() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(100);

        // When & Then
        assertThatThrownBy(() -> controller.createCompletion(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing strategy is required");
    }

    @Test
    void shouldHandleWhitespaceOnlyRoutingStrategy() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("   ");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(100);

        // When & Then
        assertThatThrownBy(() -> controller.createCompletion(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing strategy is required");
    }

    @Test
    void shouldHandleMessagesWithoutUserRole() {
        // Given
        ChatCompletionRequest.ChatMessage systemMessage = new ChatCompletionRequest.ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent("You are a helpful assistant");

        ChatCompletionRequest.ChatMessage assistantMessage = new ChatCompletionRequest.ChatMessage();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent("I can help you");

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(systemMessage, assistantMessage));
        request.setMaxTokens(100);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq("No user message found"), eq(100))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleNullMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(null);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq("Hello"), eq(null))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleZeroMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(0);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq("Hello"), eq(0))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleNegativeMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(-1);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq("Hello"), eq(-1))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleLargeMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(10000);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq("Hello"), eq(10000))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleEmptyMessageContent() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(createUserMessage("")));
        request.setMaxTokens(100);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq(""), eq(100))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleVeryLongMessageContent() {
        // Given
        String longMessage = "A".repeat(10000);
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy("PRICE");
        request.setMessages(List.of(createUserMessage(longMessage)));
        request.setMaxTokens(100);

        when(routingService.routeRequest(eq("test-user"), eq(AiRequest.RoutingStrategy.PRICE),
                eq(longMessage), eq(100))).thenReturn(aiRequest);

        // When
        ResponseEntity<ChatCompletionResponse> response = controller.createCompletion(request, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ChatCompletionRequest createValidRequest(String routingStrategy) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setRoutingStrategy(routingStrategy);
        request.setMessages(List.of(createUserMessage("Hello, how are you?")));
        request.setMaxTokens(100);
        return request;
    }

    private ChatCompletionRequest.ChatMessage createUserMessage(String content) {
        ChatCompletionRequest.ChatMessage message = new ChatCompletionRequest.ChatMessage();
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    private Provider createProvider(Long id, String name) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setName(name);
        provider.setBaseUrl("https://api." + name.toLowerCase() + ".com/v1");
        provider.setApiKey("test-key-" + name.toLowerCase());
        provider.setIsActive(true);
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());
        return provider;
    }

    private Model createModel(Long id, Provider provider, String modelId) {
        Model model = new Model();
        model.setId(id);
        model.setProvider(provider);
        model.setModelId(modelId);
        model.setName(modelId + " Model");
        model.setContextWindow(4096);
        model.setInputPricing(BigDecimal.valueOf(0.001));
        model.setOutputPricing(BigDecimal.valueOf(0.002));
        model.setIsActive(true);
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        return model;
    }
}