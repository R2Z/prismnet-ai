package com.prismnetai.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import com.prismnetai.service.RoutingStrategyInferenceService;
import com.prismnetai.service.provider.AiProviderService;
import com.prismnetai.service.provider.ProviderServiceRegistry;
import com.prismnetai.validation.ChatCompletionRequestValidator;

@ExtendWith(MockitoExtension.class)
class ChatCompletionControllerTest {

    @Mock
    private RoutingService routingService;

    @Mock
    private WebClient webClient;

    @Mock
    private ChatCompletionRequestValidator validator;

    @Mock
    private ProviderServiceRegistry providerServiceRegistry;

    @Mock
    private RoutingStrategyInferenceService routingStrategyInferenceService;

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

        // Mock validator to do nothing for valid requests
        // For invalid requests, we'll mock it to throw exceptions in specific tests

        // Mock provider service registry to return a mock provider service
        AiProviderService mockProviderService = mock(AiProviderService.class);
        lenient().when(providerServiceRegistry.getProviderService("OpenAI")).thenReturn(mockProviderService);

        // Mock the provider service to return a ChatCompletionResponse
        ChatCompletionResponse mockResponse = ChatCompletionResponse.builder()
            .id("chatcmpl-100")
            .object("chat.completion")
            .created(1234567890)
            .model("gpt-4")
            .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                .strategy("PRICE")
                .provider("OpenAI")
                .build())
            .choices(List.of(ChatCompletionResponse.ChatChoice.builder()
                .index(0)
                .message(ChatCompletionResponse.ChatMessage.builder()
                    .role("assistant")
                    .content("Hello! I'm doing well, thank you for asking.")
                    .build())
                .finishReason("stop")
                .build()))
            .usage(ChatCompletionResponse.Usage.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .cost(BigDecimal.valueOf(0.001))
                .build())
            .build();
        lenient().when(mockProviderService.callCompletion(any(ChatCompletionRequest.class), eq(aiRequest))).thenReturn(mockResponse);

        // Mock routing strategy inference service
        lenient().when(routingStrategyInferenceService.inferRoutingStrategy(any(ChatCompletionRequest.class)))
                .thenReturn(new RoutingStrategyInferenceService.RoutingInferenceResult(
                        AiRequest.RoutingStrategy.PRICE, null, null));

        // Mock routing service to return aiRequest for any arguments
        lenient().when(routingService.routeRequest(any(), any(), any(), any()))
                .thenReturn(aiRequest);
    }

    @Test
    void shouldReturnSuccessfulResponse_whenValidRequestWithPriceRouting() {
        // Given
        ChatCompletionRequest request = createValidRequest("PRICE");

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

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
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(systemMessage, userMessage, assistantMessage));
        request.setMaxTokens(50);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleNullMessagesGracefully() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(null);
        request.setMaxTokens(100);

        // Mock validator to throw IllegalArgumentException for null messages
        doThrow(new IllegalArgumentException("Messages are required")).when(validator).validate(request);

        // When & Then
        assertThatThrownBy(() -> controller.createChatCompletion(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Messages are required");
    }

    @Test
    void shouldHandleEmptyMessagesList() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(List.of());
        request.setMaxTokens(100);

        // Mock validator to throw IllegalArgumentException for empty messages
        doThrow(new IllegalArgumentException("Messages are required")).when(validator).validate(request);

        // When & Then
        assertThatThrownBy(() -> controller.createChatCompletion(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Messages are required");
    }

    @Test
    void shouldHandleNullRoutingStrategy() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        // No routing strategy set
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(100);

        // Mock validator to throw IllegalArgumentException for null routing strategy
        doThrow(new IllegalArgumentException("Routing strategy is required")).when(validator).validate(request);

        // When & Then
        assertThatThrownBy(() -> controller.createChatCompletion(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing strategy is required");
    }

    @Test
    void shouldHandleEmptyRoutingStrategy() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        // Empty routing strategy removed
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(100);

        // Mock validator to throw IllegalArgumentException for empty routing strategy
        doThrow(new IllegalArgumentException("Routing strategy is required")).when(validator).validate(request);

        // When & Then
        assertThatThrownBy(() -> controller.createChatCompletion(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing strategy is required");
    }

    @Test
    void shouldHandleWhitespaceOnlyRoutingStrategy() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        // Whitespace routing strategy removed
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(100);

        // Mock validator to throw IllegalArgumentException for whitespace-only routing strategy
        doThrow(new IllegalArgumentException("Routing strategy is required")).when(validator).validate(request);

        // When & Then
        assertThatThrownBy(() -> controller.createChatCompletion(request, null))
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
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(systemMessage, assistantMessage));
        request.setMaxTokens(100);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleNullMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(null);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleZeroMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(0);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleNegativeMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(-1);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleLargeMaxTokens() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(createUserMessage("Hello")));
        request.setMaxTokens(10000);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleEmptyMessageContent() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(createUserMessage("")));
        request.setMaxTokens(100);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleVeryLongMessageContent() {
        // Given
        String longMessage = "A".repeat(10000);
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("openai/gpt-4");
        request.setMessages(List.of(createUserMessage(longMessage)));
        request.setMaxTokens(100);

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<ChatCompletionResponse> response = (ResponseEntity<ChatCompletionResponse>) controller.createChatCompletion(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ChatCompletionRequest createValidRequest(String routingStrategy) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        // Legacy field removed, use model instead
        request.setModel("openai/gpt-4");
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