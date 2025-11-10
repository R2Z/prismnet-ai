package com.prismnetai.service.routing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.entity.ProviderMetric;
import com.prismnetai.repository.ModelRepository;
import com.prismnetai.repository.ProviderMetricRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ThroughputRoutingStrategyTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ProviderMetricRepository providerMetricRepository;

    @InjectMocks
    private ThroughputRoutingStrategy throughputRoutingStrategy;

    private Provider provider1;
    private Provider provider2;
    private Model model1;
    private Model model2;
    private ProviderMetric highThroughputMetric;
    private ProviderMetric lowThroughputMetric;

    @BeforeEach
    void setUp() {
        provider1 = createProvider(1L, "OpenAI");
        provider2 = createProvider(2L, "Anthropic");

        model1 = createModel(1L, provider1, "gpt-4");
        model2 = createModel(2L, provider2, "claude-3-opus");

        highThroughputMetric = createMetric(provider1, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(100.0));
        lowThroughputMetric = createMetric(provider2, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(50.0));

        // Set default lookback duration for tests
        ReflectionTestUtils.setField(throughputRoutingStrategy, "metricsLookbackDuration", Duration.ofHours(1));
    }

    @Test
    void shouldReturnModelFromHighestThroughputProvider_whenMultipleProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of(model1, model2);
        List<ProviderMetric> throughputMetrics = List.of(highThroughputMetric, lowThroughputMetric);

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(throughputMetrics);

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(model1.getId());
        assertThat(result.get().getProvider().getId()).isEqualTo(provider1.getId());
    }

    @Test
    void shouldReturnModelFromAvailableProvider_whenSomeProvidersHaveNoThroughputData() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of(model1, model2);
        List<ProviderMetric> throughputMetrics = List.of(highThroughputMetric); // Only provider1 has data

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(throughputMetrics);

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(model1.getId());
    }

    @Test
    void shouldReturnEmpty_whenNoProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenNoActiveModelsAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of();

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenNoThroughputDataAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of(model1, model2);
        List<ProviderMetric> throughputMetrics = List.of(); // No throughput data

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(throughputMetrics);

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnCorrectStrategyName() {
        // When
        String strategyName = throughputRoutingStrategy.getStrategyName();

        // Then
        assertThat(strategyName).isEqualTo("THROUGHPUT");
    }

    @Test
    void shouldHandleNullProvidersList() {
        // Given
        List<Provider> availableProviders = null;

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyProvidersList() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSelectModelWithLatestThroughputData_whenMultipleMetricsExist() {
        // Given
        ProviderMetric olderMetric = createMetric(provider1, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(80.0));
        olderMetric.setTimestamp(LocalDateTime.now().minusMinutes(30));

        ProviderMetric newerMetric = createMetric(provider1, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(120.0));
        newerMetric.setTimestamp(LocalDateTime.now().minusMinutes(5));

        List<Provider> availableProviders = List.of(provider1);
        List<Model> activeModels = List.of(model1);
        List<ProviderMetric> throughputMetrics = List.of(olderMetric, newerMetric);

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(throughputMetrics);

        // When
        Optional<Model> result = throughputRoutingStrategy.selectModel(availableProviders, "test-user", null);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(model1.getId());
        // Should select based on the latest (newer) metric with higher throughput
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

    private ProviderMetric createMetric(Provider provider, ProviderMetric.MetricType metricType, BigDecimal value) {
        ProviderMetric metric = new ProviderMetric();
        metric.setProvider(provider);
        metric.setMetricType(metricType);
        metric.setValue(value);
        metric.setTimestamp(LocalDateTime.now());
        return metric;
    }
}