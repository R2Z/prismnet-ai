package com.prismnetai.service.routing;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.repository.ModelRepository;
import com.prismnetai.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreferredModelRoutingStrategyTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private Provider provider1;

    @Mock
    private Provider provider2;

    @Mock
    private Model model1;

    @Mock
    private Model model2;

    @Mock
    private Model model3;

    private PreferredModelRoutingStrategy strategy;
    private List<Provider> availableProviders;

    @BeforeEach
    void setUp() {
        strategy = new PreferredModelRoutingStrategy(modelRepository, providerRepository);

        // Setup providers
        when(provider1.getId()).thenReturn(1L);
        when(provider1.getName()).thenReturn("OpenAI");
        when(provider2.getId()).thenReturn(2L);
        when(provider2.getName()).thenReturn("Anthropic");

        availableProviders = List.of(provider1, provider2);

        // Setup models
        when(model1.getModelId()).thenReturn("gpt-4");
        when(model1.getProvider()).thenReturn(provider1);

        when(model2.getModelId()).thenReturn("gpt-4");
        when(model2.getProvider()).thenReturn(provider2);

        when(model3.getModelId()).thenReturn("claude-3");
        when(model3.getProvider()).thenReturn(provider1);
    }

    @Test
    void selectModel_shouldReturnFirstMatchingModel_whenPreferredModelFound() {
        // Given
        String preferredModel = "gpt-4";
        List<Model> matchingModels = List.of(model1, model2); // model1 from provider1 comes first

        when(modelRepository.findActiveModelsByModelIdAndProviderIds(eq(preferredModel), any(List.class)))
                .thenReturn(matchingModels);

        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", preferredModel);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(model1); // Should return first model (from first provider)
    }

    @Test
    void selectModel_shouldReturnEmpty_whenPreferredModelNotFound() {
        // Given
        String preferredModel = "non-existent-model";

        when(modelRepository.findActiveModelsByModelIdAndProviderIds(eq(preferredModel), any(List.class)))
                .thenReturn(List.of());

        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", preferredModel);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnEmpty_whenPreferredModelIsNull() {
        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnEmpty_whenPreferredModelIsEmpty() {
        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", "");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnEmpty_whenNoProvidersAvailable() {
        // When
        Optional<Model> result = strategy.selectModel(List.of(), "user1", "gpt-4");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnEmpty_whenProvidersIsNull() {
        // When
        Optional<Model> result = strategy.selectModel(null, "user1", "gpt-4");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnSingleMatchingModel() {
        // Given
        String preferredModel = "claude-3";
        List<Model> matchingModels = List.of(model3);

        when(modelRepository.findActiveModelsByModelIdAndProviderIds(eq(preferredModel), any(List.class)))
                .thenReturn(matchingModels);

        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", preferredModel);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(model3);
    }

    @Test
    void selectModel_shouldReturnModel_whenProviderModelFormatUsedAndProviderFound() {
        // Given
        String preferredModel = "OpenAI/gpt-4";

        when(providerRepository.findByName("OpenAI")).thenReturn(Optional.of(provider1));
        when(modelRepository.findActiveByModelIdAndProvider("gpt-4", provider1)).thenReturn(Optional.of(model1));

        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", preferredModel);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(model1);
    }

    @Test
    void selectModel_shouldReturnEmpty_whenProviderModelFormatUsedAndProviderNotFound() {
        // Given
        String preferredModel = "Unknown/gpt-4";

        when(providerRepository.findByName("Unknown")).thenReturn(Optional.empty());

        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", preferredModel);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnEmpty_whenProviderModelFormatUsedAndProviderNotAvailable() {
        // Given
        String preferredModel = "OpenAI/gpt-4";
        Provider unavailableProvider = provider2; // provider2 is not in availableProviders for this test

        when(providerRepository.findByName("OpenAI")).thenReturn(Optional.of(unavailableProvider));

        // When
        Optional<Model> result = strategy.selectModel(List.of(provider1), "user1", preferredModel);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnEmpty_whenProviderModelFormatUsedAndModelNotFound() {
        // Given
        String preferredModel = "OpenAI/unknown-model";

        when(providerRepository.findByName("OpenAI")).thenReturn(Optional.of(provider1));
        when(modelRepository.findActiveByModelIdAndProvider("unknown-model", provider1)).thenReturn(Optional.empty());

        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", preferredModel);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void selectModel_shouldReturnEmpty_whenInvalidProviderModelFormat() {
        // Given
        String preferredModel = "invalid/format/extra";

        // When
        Optional<Model> result = strategy.selectModel(availableProviders, "user1", preferredModel);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getStrategyName_shouldReturnPreferredModel() {
        // When
        String strategyName = strategy.getStrategyName();

        // Then
        assertThat(strategyName).isEqualTo("PREFERRED_MODEL");
    }
}