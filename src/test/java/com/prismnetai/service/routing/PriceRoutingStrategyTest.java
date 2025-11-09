package com.prismnetai.service.routing;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.repository.ModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceRoutingStrategyTest {

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private PriceRoutingStrategy priceRoutingStrategy;

    private Provider provider1;
    private Provider provider2;
    private Model cheapModel;
    private Model expensiveModel;
    private Model cheapestModel;

    @BeforeEach
    void setUp() {
        provider1 = createProvider(1L, "OpenAI");
        provider2 = createProvider(2L, "Anthropic");

        // Models ordered by cost: cheapestModel (0.001) < cheapModel (0.002) < expensiveModel (0.005)
        cheapestModel = createModel(1L, provider1, "gpt-4", BigDecimal.valueOf(0.0005), BigDecimal.valueOf(0.0005));
        cheapModel = createModel(2L, provider1, "gpt-3.5-turbo", BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        expensiveModel = createModel(3L, provider2, "claude-3-opus", BigDecimal.valueOf(0.0025), BigDecimal.valueOf(0.0025));
    }

    @Test
    void shouldReturnCheapestModel_whenMultipleProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> modelsOrderedByCost = List.of(cheapestModel, cheapModel, expensiveModel);

        when(modelRepository.findActiveModelsOrderedByLowestCost()).thenReturn(modelsOrderedByCost);

        // When
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(cheapestModel.getId());
        assertThat(result.get().getModelId()).isEqualTo("gpt-4");
    }

    @Test
    void shouldReturnCheapestAvailableModel_whenSomeProvidersUnavailable() {
        // Given
        List<Provider> availableProviders = List.of(provider2); // Only Anthropic available
        List<Model> modelsOrderedByCost = List.of(cheapestModel, cheapModel, expensiveModel);

        when(modelRepository.findActiveModelsOrderedByLowestCost()).thenReturn(modelsOrderedByCost);

        // When
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(expensiveModel.getId());
        assertThat(result.get().getModelId()).isEqualTo("claude-3-opus");
    }

    @Test
    void shouldReturnEmpty_whenNoProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenNoActiveModelsAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> modelsOrderedByCost = List.of();

        when(modelRepository.findActiveModelsOrderedByLowestCost()).thenReturn(modelsOrderedByCost);

        // When
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnCheapestModel_whenAllModelsFromSameProvider() {
        // Given
        List<Provider> availableProviders = List.of(provider1);
        List<Model> modelsOrderedByCost = List.of(cheapestModel, cheapModel); // Both from provider1

        when(modelRepository.findActiveModelsOrderedByLowestCost()).thenReturn(modelsOrderedByCost);

        // When
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(cheapestModel.getId());
    }

    @Test
    void shouldReturnCorrectStrategyName() {
        // When
        String strategyName = priceRoutingStrategy.getStrategyName();

        // Then
        assertThat(strategyName).isEqualTo("PRICE");
    }

    @Test
    void shouldHandleNullProvidersList() {
        // Given
        List<Provider> availableProviders = null;

        // When & Then
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyProvidersList() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSelectModelWithLowestCombinedCost_whenInputAndOutputPricingDiffer() {
        // Given - Models with different input/output pricing combinations
        Model model1 = createModel(1L, provider1, "model1", BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.002)); // Total: 0.003
        Model model2 = createModel(2L, provider1, "model2", BigDecimal.valueOf(0.002), BigDecimal.valueOf(0.001)); // Total: 0.003
        Model model3 = createModel(3L, provider1, "model3", BigDecimal.valueOf(0.0005), BigDecimal.valueOf(0.0005)); // Total: 0.001

        List<Provider> availableProviders = List.of(provider1);
        List<Model> modelsOrderedByCost = List.of(model3, model1, model2); // model3 has lowest total cost

        when(modelRepository.findActiveModelsOrderedByLowestCost()).thenReturn(modelsOrderedByCost);

        // When
        Optional<Model> result = priceRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(model3.getId());
        assertThat(result.get().getModelId()).isEqualTo("model3");
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

    private Model createModel(Long id, Provider provider, String modelId, BigDecimal inputPricing, BigDecimal outputPricing) {
        Model model = new Model();
        model.setId(id);
        model.setProvider(provider);
        model.setModelId(modelId);
        model.setName(modelId + " Model");
        model.setContextWindow(4096);
        model.setInputPricing(inputPricing);
        model.setOutputPricing(outputPricing);
        model.setIsActive(true);
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        return model;
    }
}