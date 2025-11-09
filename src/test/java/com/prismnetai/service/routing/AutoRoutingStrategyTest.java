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
class AutoRoutingStrategyTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ProviderMetricRepository providerMetricRepository;

    @InjectMocks
    private AutoRoutingStrategy autoRoutingStrategy;

    private Provider provider1;
    private Provider provider2;
    private Model cheapModel;
    private Model expensiveModel;
    private ProviderMetric highThroughputMetric;
    private ProviderMetric lowLatencyMetric;
    private ProviderMetric highSuccessRateMetric;

    @BeforeEach
    void setUp() {
        provider1 = createProvider(1L, "OpenAI");
        provider2 = createProvider(2L, "Anthropic");

        // Models with different pricing
        cheapModel = createModel(1L, provider1, "gpt-4", BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.002)); // Total: 0.003
        expensiveModel = createModel(2L, provider2, "claude-3-opus", BigDecimal.valueOf(0.002), BigDecimal.valueOf(0.003)); // Total: 0.005

        // Metrics for provider1 (better performance)
        highThroughputMetric = createMetric(provider1, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(1000.0));
        lowLatencyMetric = createMetric(provider1, ProviderMetric.MetricType.LATENCY, BigDecimal.valueOf(500.0));
        highSuccessRateMetric = createMetric(provider1, ProviderMetric.MetricType.SUCCESS_RATE, BigDecimal.valueOf(0.98));

        // Provider2 will have worse metrics (not set in all tests)

        // Set default lookback duration for tests
        ReflectionTestUtils.setField(autoRoutingStrategy, "metricsLookbackDuration", Duration.ofHours(1));
    }

    @Test
    void shouldReturnModelWithHighestAutoScore_whenMultipleProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of(cheapModel, expensiveModel);
        List<ProviderMetric> metrics = List.of(highThroughputMetric, lowLatencyMetric, highSuccessRateMetric);

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(metrics);

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        // cheapModel should win due to lower price + good metrics
        assertThat(result.get().getId()).isEqualTo(cheapModel.getId());
        assertThat(result.get().getProvider().getId()).isEqualTo(provider1.getId());
    }

    @Test
    void shouldCalculateScoreCorrectly_whenAllMetricsAvailable() {
        // Given - Provider1 has excellent metrics, Provider2 has poor metrics
        ProviderMetric poorThroughput = createMetric(provider2, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(100.0));
        ProviderMetric highLatency = createMetric(provider2, ProviderMetric.MetricType.LATENCY, BigDecimal.valueOf(2000.0));
        ProviderMetric lowSuccessRate = createMetric(provider2, ProviderMetric.MetricType.SUCCESS_RATE, BigDecimal.valueOf(0.85));

        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of(cheapModel, expensiveModel);
        List<ProviderMetric> metrics = List.of(highThroughputMetric, lowLatencyMetric, highSuccessRateMetric,
                                              poorThroughput, highLatency, lowSuccessRate);

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(metrics);

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        // Should select the model with better overall score (provider1)
        assertThat(result.get().getProvider().getId()).isEqualTo(provider1.getId());
    }

    @Test
    void shouldHandleMissingMetricsGracefully() {
        // Given - Only some metrics available
        List<Provider> availableProviders = List.of(provider1, provider2);
        List<Model> activeModels = List.of(cheapModel, expensiveModel);
        List<ProviderMetric> metrics = List.of(highThroughputMetric); // Only throughput available

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(metrics);

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        // Should still select a model, using available metrics and price
    }

    @Test
    void shouldReturnEmpty_whenNoProvidersAvailable() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

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
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnCorrectStrategyName() {
        // When
        String strategyName = autoRoutingStrategy.getStrategyName();

        // Then
        assertThat(strategyName).isEqualTo("AUTO");
    }

    @Test
    void shouldHandleNullProvidersList() {
        // Given
        List<Provider> availableProviders = null;

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyProvidersList() {
        // Given
        List<Provider> availableProviders = List.of();

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldPreferHigherScoringModel_whenMetricsAreEqual() {
        // Given - Two models with same metrics but different pricing
        Model cheaperModel = createModel(3L, provider1, "gpt-3.5", BigDecimal.valueOf(0.0005), BigDecimal.valueOf(0.001)); // Total: 0.0015
        List<Provider> availableProviders = List.of(provider1);
        List<Model> activeModels = List.of(cheapModel, cheaperModel);
        List<ProviderMetric> metrics = List.of(highThroughputMetric, lowLatencyMetric, highSuccessRateMetric);

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(metrics);

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        // Should select the cheaper model due to price advantage
        assertThat(result.get().getId()).isEqualTo(cheaperModel.getId());
    }

    @Test
    void shouldUseLatestMetrics_whenMultipleMetricsExistForSameProviderAndType() {
        // Given - Multiple metrics of same type, should use latest
        ProviderMetric olderThroughput = createMetric(provider1, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(500.0));
        olderThroughput.setTimestamp(LocalDateTime.now().minusMinutes(30));

        ProviderMetric newerThroughput = createMetric(provider1, ProviderMetric.MetricType.THROUGHPUT, BigDecimal.valueOf(1200.0));
        newerThroughput.setTimestamp(LocalDateTime.now().minusMinutes(5));

        List<Provider> availableProviders = List.of(provider1);
        List<Model> activeModels = List.of(cheapModel);
        List<ProviderMetric> metrics = List.of(olderThroughput, newerThroughput, lowLatencyMetric, highSuccessRateMetric);

        when(modelRepository.findActiveModelsByProviderIds(anyList())).thenReturn(activeModels);
        when(providerMetricRepository.findRecentMetricsByProvidersAndType(anyList(), any(), any())).thenReturn(metrics);

        // When
        Optional<Model> result = autoRoutingStrategy.selectModel(availableProviders);

        // Then
        assertThat(result).isPresent();
        // Should use the newer, higher throughput metric in scoring
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

    private ProviderMetric createMetric(Provider provider, ProviderMetric.MetricType metricType, BigDecimal value) {
        ProviderMetric metric = new ProviderMetric();
        metric.setProvider(provider);
        metric.setMetricType(metricType);
        metric.setValue(value);
        metric.setTimestamp(LocalDateTime.now());
        return metric;
    }
}