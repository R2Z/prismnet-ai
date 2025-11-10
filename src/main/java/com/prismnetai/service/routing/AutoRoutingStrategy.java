package com.prismnetai.service.routing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.prismnetai.entity.Model;
import com.prismnetai.entity.Provider;
import com.prismnetai.entity.ProviderMetric;
import com.prismnetai.repository.ModelRepository;
import com.prismnetai.repository.ProviderMetricRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("AUTO")
@RequiredArgsConstructor
public class AutoRoutingStrategy implements RoutingStrategy {

    private final ModelRepository modelRepository;
    private final ProviderMetricRepository providerMetricRepository;
    private final Duration metricsLookbackDuration;

    // Weights for the scoring algorithm (configurable)
    private static final double PRICE_WEIGHT = 0.3;
    private static final double THROUGHPUT_WEIGHT = 0.25;
    private static final double LATENCY_WEIGHT = 0.25;
    private static final double SUCCESS_RATE_WEIGHT = 0.2;

    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders, String userId) {
        log.info("AutoRoutingStrategy.selectModel() - Starting auto intelligent model selection");

        if (availableProviders == null) {
            log.warn("AutoRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        if (availableProviders.isEmpty()) {
            log.warn("AutoRoutingStrategy.selectModel() - Empty providers list provided");
            return Optional.empty();
        }

        log.debug("AutoRoutingStrategy.selectModel() - Evaluating {} available providers: {}",
                  availableProviders.size(),
                  availableProviders.stream().map(Provider::getName).toList());

        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();

        // Get active models
        List<Model> activeModels = modelRepository.findActiveModelsByProviderIds(providerIds);

        if (activeModels.isEmpty()) {
            log.warn("AutoRoutingStrategy.selectModel() - No active models found for available providers");
            return Optional.empty();
        }

        // Get latest metrics for all providers
        LocalDateTime since = LocalDateTime.now().minus(metricsLookbackDuration);
        List<ProviderMetric> recentMetrics = providerMetricRepository
                .findRecentMetricsByProvidersAndType(providerIds, null, since);

        // Group metrics by provider and metric type
        Map<Long, Map<ProviderMetric.MetricType, ProviderMetric>> metricsByProvider = recentMetrics.stream()
                .collect(Collectors.groupingBy(
                    pm -> pm.getProvider().getId(),
                    Collectors.toMap(
                        ProviderMetric::getMetricType,
                        pm -> pm,
                        (a, b) -> a.getTimestamp().isAfter(b.getTimestamp()) ? a : b // Keep latest
                    )
                ));

        // Calculate scores for each model
        List<ModelScore> modelScores = activeModels.stream()
                .map(model -> calculateModelScore(model, metricsByProvider.get(model.getProvider().getId())))
                .sorted(Comparator.comparingDouble(ModelScore::getScore).reversed()) // Higher score first
                .collect(Collectors.toList());

        log.debug("AutoRoutingStrategy.selectModel() - Calculated scores for {} models",
                  modelScores.size());

        Optional<ModelScore> bestModelScore = modelScores.stream().findFirst();

        if (bestModelScore.isPresent()) {
            Model selectedModel = bestModelScore.get().getModel();
            log.info("AutoRoutingStrategy.selectModel() - Selected model: {} from provider {} with auto score: {:.3f}",
                     selectedModel.getModelId(), selectedModel.getProvider().getName(),
                     bestModelScore.get().getScore());
        } else {
            log.warn("AutoRoutingStrategy.selectModel() - No suitable model found with calculated scores");
        }

        log.info("AutoRoutingStrategy.selectModel() - Completed auto intelligent model selection");
        return bestModelScore.map(ModelScore::getModel);
    }

    private ModelScore calculateModelScore(Model model, Map<ProviderMetric.MetricType, ProviderMetric> metrics) {
        double score = 0.0;

        // Price score (lower price = higher score, normalized)
        BigDecimal totalCost = model.getInputPricing().add(model.getOutputPricing());
        double priceScore = Math.max(0, 1.0 - totalCost.doubleValue() / 0.01); // Normalize assuming max cost of 0.01
        score += PRICE_WEIGHT * priceScore;

        // Throughput score (higher throughput = higher score, normalized)
        if (metrics != null && metrics.containsKey(ProviderMetric.MetricType.THROUGHPUT)) {
            double throughput = metrics.get(ProviderMetric.MetricType.THROUGHPUT).getValue().doubleValue();
            double throughputScore = Math.min(1.0, throughput / 1000.0); // Normalize assuming max throughput of 1000
            score += THROUGHPUT_WEIGHT * throughputScore;
        }

        // Latency score (lower latency = higher score, normalized)
        if (metrics != null && metrics.containsKey(ProviderMetric.MetricType.LATENCY)) {
            double latency = metrics.get(ProviderMetric.MetricType.LATENCY).getValue().doubleValue();
            double latencyScore = Math.max(0, 1.0 - latency / 5000.0); // Normalize assuming max acceptable latency of 5000ms
            score += LATENCY_WEIGHT * latencyScore;
        }

        // Success rate score (higher success rate = higher score)
        if (metrics != null && metrics.containsKey(ProviderMetric.MetricType.SUCCESS_RATE)) {
            double successRate = metrics.get(ProviderMetric.MetricType.SUCCESS_RATE).getValue().doubleValue();
            score += SUCCESS_RATE_WEIGHT * successRate;
        }

        log.debug("AutoRoutingStrategy.calculateModelScore() - Model {} score breakdown: price={:.3f}, throughput={:.3f}, latency={:.3f}, success={:.3f}, total={:.3f}",
                  model.getModelId(), priceScore, 
                  metrics != null && metrics.containsKey(ProviderMetric.MetricType.THROUGHPUT) ? 
                      Math.min(1.0, metrics.get(ProviderMetric.MetricType.THROUGHPUT).getValue().doubleValue() / 1000.0) : 0.0,
                  metrics != null && metrics.containsKey(ProviderMetric.MetricType.LATENCY) ? 
                      Math.max(0, 1.0 - metrics.get(ProviderMetric.MetricType.LATENCY).getValue().doubleValue() / 5000.0) : 0.0,
                  metrics != null && metrics.containsKey(ProviderMetric.MetricType.SUCCESS_RATE) ? 
                      metrics.get(ProviderMetric.MetricType.SUCCESS_RATE).getValue().doubleValue() : 0.0,
                  score);

        return new ModelScore(model, score);
    }

    @Override
    public String getStrategyName() {
        return "AUTO";
    }

    private static class ModelScore {
        private final Model model;
        private final double score;

        public ModelScore(Model model, double score) {
            this.model = model;
            this.score = score;
        }

        public Model getModel() {
            return model;
        }

        public double getScore() {
            return score;
        }
    }
}