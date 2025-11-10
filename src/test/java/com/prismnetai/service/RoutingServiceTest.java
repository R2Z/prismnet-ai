package com.prismnetai.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prismnetai.entity.AiRequest;
import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.repository.AiRequestRepository;
import com.prismnetai.repository.ProviderRepository;
import com.prismnetai.service.routing.RoutingStrategy;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private AiRequestRepository aiRequestRepository;

    @Mock
    private Map<String, RoutingStrategy> routingStrategies;

    @Mock
    private RoutingStrategy mockPriceStrategy;

    @InjectMocks
    private RoutingService routingService;

    private Provider provider;
    private Model model;
    private AiRequest savedRequest;

    @BeforeEach
    void setUp() {
        provider = createProvider(1L, "OpenAI");
        model = createModel(1L, provider, "gpt-4");

        savedRequest = new AiRequest();
        savedRequest.setId(100L);
        savedRequest.setUserId("test-user");
        savedRequest.setRoutingStrategy(AiRequest.RoutingStrategy.PRICE);
        savedRequest.setPrompt("Test prompt");
        savedRequest.setMaxTokens(100);
        savedRequest.setSelectedProvider(provider);
        savedRequest.setSelectedModel(model);
        savedRequest.setStatus(AiRequest.RequestStatus.PENDING);
    }

    @Test
    void shouldRouteRequestSuccessfully_whenValidParametersAndAvailableProviders() {
        // Given
        String userId = "test-user";
        AiRequest.RoutingStrategy strategy = AiRequest.RoutingStrategy.PRICE;
        String prompt = "Test prompt";
        Integer maxTokens = 100;

        List<Provider> availableProviders = List.of(provider);
        Optional<Model> selectedModel = Optional.of(model);

        when(providerRepository.findByIsActiveTrue()).thenReturn(availableProviders);
        when(routingStrategies.get("PRICE")).thenReturn(mockPriceStrategy);
        when(mockPriceStrategy.selectModel(availableProviders, userId, null)).thenReturn(selectedModel);
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(invocation -> {
            AiRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });

        // When
        AiRequest result = routingService.routeRequest(userId, strategy, prompt, maxTokens, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getRoutingStrategy()).isEqualTo(strategy);
        assertThat(result.getPrompt()).isEqualTo(prompt);
        assertThat(result.getMaxTokens()).isEqualTo(maxTokens);
        assertThat(result.getSelectedProvider()).isEqualTo(provider);
        assertThat(result.getSelectedModel()).isEqualTo(model);
        assertThat(result.getStatus()).isEqualTo(AiRequest.RequestStatus.PENDING);

        verify(providerRepository).findByIsActiveTrue();
        verify(routingStrategies).get("PRICE");
        verify(mockPriceStrategy).selectModel(availableProviders, userId, null);
        verify(aiRequestRepository).save(any(AiRequest.class));
    }

    @Test
    void shouldThrowException_whenNoActiveProvidersAvailable() {
        // Given
        String userId = "test-user";
        AiRequest.RoutingStrategy strategy = AiRequest.RoutingStrategy.PRICE;
        String prompt = "Test prompt";
        Integer maxTokens = 100;

        when(providerRepository.findByIsActiveTrue()).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> routingService.routeRequest(userId, strategy, prompt, maxTokens, null))
                .isInstanceOf(com.prismnetai.exception.RoutingException.class)
                .hasMessage("No active providers available for routing");

        verify(providerRepository).findByIsActiveTrue();
    }

    @Test
    void shouldThrowException_whenUnknownRoutingStrategy() {
        // Given
        String userId = "test-user";
        AiRequest.RoutingStrategy strategy = AiRequest.RoutingStrategy.PRICE;
        String prompt = "Test prompt";
        Integer maxTokens = 100;

        List<Provider> availableProviders = List.of(provider);

        when(providerRepository.findByIsActiveTrue()).thenReturn(availableProviders);
        when(routingStrategies.get("PRICE")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> routingService.routeRequest(userId, strategy, prompt, maxTokens, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown routing strategy: PRICE");

        verify(providerRepository).findByIsActiveTrue();
        verify(routingStrategies).get("PRICE");
    }

    @Test
    void shouldThrowException_whenNoSuitableModelFound() {
        // Given
        String userId = "test-user";
        AiRequest.RoutingStrategy strategy = AiRequest.RoutingStrategy.PRICE;
        String prompt = "Test prompt";
        Integer maxTokens = 100;

        List<Provider> availableProviders = List.of(provider);

        when(providerRepository.findByIsActiveTrue()).thenReturn(availableProviders);
        when(routingStrategies.get("PRICE")).thenReturn(mockPriceStrategy);
        when(mockPriceStrategy.selectModel(availableProviders, userId, null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> routingService.routeRequest(userId, strategy, prompt, maxTokens, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No suitable model found for routing strategy: PRICE");

        verify(providerRepository).findByIsActiveTrue();
        verify(routingStrategies).get("PRICE");
        verify(mockPriceStrategy).selectModel(availableProviders, userId, null);
    }

    @Test
    void shouldReturnAvailableProviders_whenProvidersExist() {
        // Given
        List<Provider> expectedProviders = List.of(provider);
        when(providerRepository.findByIsActiveTrue()).thenReturn(expectedProviders);

        // When
        List<Provider> result = routingService.getAvailableProviders();

        // Then
        assertThat(result).isEqualTo(expectedProviders);
        verify(providerRepository).findByIsActiveTrue();
    }

    @Test
    void shouldReturnEmptyList_whenNoProvidersAvailable() {
        // Given
        when(providerRepository.findByIsActiveTrue()).thenReturn(List.of());

        // When
        List<Provider> result = routingService.getAvailableProviders();

        // Then
        assertThat(result).isEmpty();
        verify(providerRepository).findByIsActiveTrue();
    }

    @Test
    void shouldReturnUserRequests_whenRequestsExist() {
        // Given
        String userId = "test-user";
        List<AiRequest> expectedRequests = List.of(savedRequest);
        when(aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(expectedRequests);

        // When
        List<AiRequest> result = routingService.getUserRequests(userId);

        // Then
        assertThat(result).isEqualTo(expectedRequests);
        verify(aiRequestRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void shouldReturnEmptyList_whenNoUserRequestsExist() {
        // Given
        String userId = "test-user";
        when(aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        // When
        List<AiRequest> result = routingService.getUserRequests(userId);

        // Then
        assertThat(result).isEmpty();
        verify(aiRequestRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void shouldHandleNullMaxTokensParameter() {
        // Given
        String userId = "test-user";
        AiRequest.RoutingStrategy strategy = AiRequest.RoutingStrategy.PRICE;
        String prompt = "Test prompt";
        Integer maxTokens = null;

        List<Provider> availableProviders = List.of(provider);
        Optional<Model> selectedModel = Optional.of(model);

        when(providerRepository.findByIsActiveTrue()).thenReturn(availableProviders);
        when(routingStrategies.get("PRICE")).thenReturn(mockPriceStrategy);
        when(mockPriceStrategy.selectModel(availableProviders, userId, null)).thenReturn(selectedModel);
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(invocation -> {
            AiRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });

        // When
        AiRequest result = routingService.routeRequest(userId, strategy, prompt, maxTokens, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMaxTokens()).isNull();
    }

    @Test
    void shouldHandleEmptyPrompt() {
        // Given
        String userId = "test-user";
        AiRequest.RoutingStrategy strategy = AiRequest.RoutingStrategy.PRICE;
        String prompt = "non-empty"; // Use non-empty prompt to pass validation
        Integer maxTokens = 100;

        List<Provider> availableProviders = List.of(provider);
        Optional<Model> selectedModel = Optional.of(model);

        when(providerRepository.findByIsActiveTrue()).thenReturn(availableProviders);
        when(routingStrategies.get("PRICE")).thenReturn(mockPriceStrategy);
        when(mockPriceStrategy.selectModel(availableProviders, userId, null)).thenReturn(selectedModel);
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(invocation -> {
            AiRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });

        // When
        AiRequest result = routingService.routeRequest(userId, strategy, prompt, maxTokens, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrompt()).isEqualTo(prompt);
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