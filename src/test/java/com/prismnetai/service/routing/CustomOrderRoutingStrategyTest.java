package com.prismnetai.service.routing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.entity.RoutingRule;
import com.prismnetai.repository.ModelRepository;
import com.prismnetai.repository.RoutingRuleRepository;

@ExtendWith(MockitoExtension.class)
class CustomOrderRoutingStrategyTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private RoutingRuleRepository routingRuleRepository;

    @InjectMocks
    private CustomOrderRoutingStrategy customOrderRoutingStrategy;

    private Provider provider1;
    private Provider provider2;
    private Provider provider3;
    private Model model1;
    private Model model2;
    private Model model3;
    private RoutingRule routingRule;

    @BeforeEach
    void setUp() {
        provider1 = createProvider(1L, "OpenAI");
        provider2 = createProvider(2L, "Anthropic");
        provider3 = createProvider(3L, "Google");

        model1 = createModel(1L, provider1, "gpt-4");
        model2 = createModel(2L, provider2, "claude-3-opus");
        model3 = createModel(3L, provider3, "gemini-pro");

        routingRule = createRoutingRule("OpenAI,Anthropic,Google");
    }

    @Test
    void shouldReturnModelFromHighestPriorityProvider_whenMultipleProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2, provider3);
        List<Model> activeModels = List.of(model1, model2, model3);
        List<RoutingRule> activeRules = List.of(routingRule);

        when(routingRuleRepository.findActiveRulesByUserIdOrderedById(anyString())).thenReturn(activeRules);
        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(model1.getId()); // OpenAI is first in the order
        assertThat(result.get().getProvider().getName()).isEqualTo("OpenAI");
    }

    @Test
    void shouldReturnModelFromNextPriorityProvider_whenHighestPriorityProviderUnavailable() {
        // Given
        List<Provider> availableProviders = List.of(provider2, provider3); // OpenAI not available
        List<Model> activeModels = List.of(model2, model3);
        List<RoutingRule> activeRules = List.of(routingRule);

        when(routingRuleRepository.findActiveRulesByUserIdOrderedById(anyString())).thenReturn(activeRules);
        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(model2.getId()); // Anthropic is second in the order
        assertThat(result.get().getProvider().getName()).isEqualTo("Anthropic");
    }

    @Test
    void shouldReturnEmpty_whenNoProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenNoActiveModelsAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of();
        List<RoutingRule> activeRules = List.of(routingRule);

        when(routingRuleRepository.findActiveRulesByUserIdOrderedById(anyString())).thenReturn(activeRules);
        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenNoActiveRoutingRulesAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<RoutingRule> activeRules = List.of();

        when(routingRuleRepository.findActiveRulesByUserIdOrderedById(anyString())).thenReturn(activeRules);

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenNoProvidersMatchRoutingRule() {
        // Given
        Provider otherProvider = createProvider(4L, "OtherProvider");
        List<Provider> availableProviders = List.of(otherProvider); // Provider not in routing rule
        List<Model> activeModels = List.of(createModel(4L, otherProvider, "other-model"));
        List<RoutingRule> activeRules = List.of(routingRule);

        when(routingRuleRepository.findActiveRulesByUserIdOrderedById(anyString())).thenReturn(activeRules);
        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnCorrectStrategyName() {
        // When
        String strategyName = customOrderRoutingStrategy.getStrategyName();

        // Then
        assertThat(strategyName).isEqualTo("CUSTOM_ORDER");
    }

    @Test
    void shouldHandleNullProvidersList() {
        // Given
        List<Provider> availableProviders = null;

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyProvidersList() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleRoutingRuleWithSpacesInProviderOrder() {
        // Given
        RoutingRule ruleWithSpaces = createRoutingRule("OpenAI, Anthropic, Google");
        List<Provider> availableProviders = List.of(provider1, provider2, provider3);
        List<Model> activeModels = List.of(model1, model2, model3);
        List<RoutingRule> activeRules = List.of(ruleWithSpaces);

        when(routingRuleRepository.findActiveRulesByUserIdOrderedById(anyString())).thenReturn(activeRules);
        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProvider().getName()).isEqualTo("OpenAI");
    }

    @Test
    void shouldUseFirstActiveRule_whenMultipleRulesExist() {
        // Given
        RoutingRule rule1 = createRoutingRule("Anthropic,OpenAI,Google"); // Different order
        RoutingRule rule2 = createRoutingRule("OpenAI,Anthropic,Google");
        List<Provider> availableProviders = List.of(provider1, provider2, provider3);
        List<Model> activeModels = List.of(model1, model2, model3);
        List<RoutingRule> activeRules = List.of(rule1, rule2); // rule1 comes first

        when(routingRuleRepository.findActiveRulesByUserIdOrderedById(anyString())).thenReturn(activeRules);
        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);

        // When
        Optional<Model> result = customOrderRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProvider().getName()).isEqualTo("Anthropic"); // Follows rule1 order
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

    private RoutingRule createRoutingRule(String providerOrder) {
        RoutingRule rule = new RoutingRule();
        rule.setId(1L);
        rule.setUserId("default");
        rule.setName("Test Rule");
        rule.setDescription("Test routing rule");
        rule.setProviderOrder(providerOrder);
        rule.setIsActive(true);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }
}