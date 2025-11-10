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

    /**
     * Selects the best model using an intelligent auto-routing algorithm that considers
     * multiple factors: price, throughput, latency, and success rate.
     *
     * @param availableProviders list of available providers to choose from
     * @param userId the user ID (can be null, not used in auto routing)
     * @return Optional containing the selected model, empty if no suitable model found
     */
    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders, String userId, String preferredModel) {
        log.info("AutoRoutingStrategy.selectModel() - Starting auto intelligent model selection");

        // Input validation
        if (availableProviders == null || availableProviders.isEmpty()) {
            log.warn("AutoRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        log.info("AutoRoutingStrategy.selectModel() - Evaluating {} available providers: {}",
                  availableProviders.size(),
                  availableProviders.stream().map(Provider::getName).toList());

        // Extract provider IDs for efficient querying
        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();

        // Get active models for available providers
        List<Model> activeModels = modelRepository.findActiveModelsByProviderIds(providerIds);
        if (activeModels.isEmpty()) {
            log.warn("AutoRoutingStrategy.selectModel() - No active models found for available providers");
            return Optional.empty();
        }

        // Get recent metrics for scoring
        Map<Long, Map<ProviderMetric.MetricType, ProviderMetric>> metricsByProvider =
            getMetricsByProvider(providerIds);

        // Calculate and rank models by score
        Optional<ModelScore> bestModelScore = activeModels.stream()
                .map(model -> calculateModelScore(model, metricsByProvider.get(model.getProvider().getId())))
                .filter(modelScore -> modelScore.score() > 0.0) // Only consider models with positive scores
                .max(Comparator.comparingDouble(ModelScore::score)); // Get highest score

        if (bestModelScore.isPresent()) {
            Model selectedModel = bestModelScore.get().model();
            log.info("AutoRoutingStrategy.selectModel() - Selected model: {} from provider {} with auto score: {:.3f}",
                     selectedModel.getModelId(), selectedModel.getProvider().getName(),
                     bestModelScore.get().score());
        } else {
            log.warn("AutoRoutingStrategy.selectModel() - No suitable model found with positive scores");
        }

        log.info("AutoRoutingStrategy.selectModel() - Completed auto intelligent model selection");
        return bestModelScore.map(ModelScore::model);
    }

    /**
     * Retrieves and organizes metrics by provider for efficient scoring.
     */
    private Map<Long, Map<ProviderMetric.MetricType, ProviderMetric>> getMetricsByProvider(List<Long> providerIds) {
        LocalDateTime since = LocalDateTime.now().minus(metricsLookbackDuration);
        List<ProviderMetric> recentMetrics = providerMetricRepository
                .findRecentMetricsByProvidersAndType(providerIds, null, since);

        return recentMetrics.stream()
                .collect(Collectors.groupingBy(
                    pm -> pm.getProvider().getId(),
                    Collectors.toMap(
                        ProviderMetric::getMetricType,
                        pm -> pm,
                        (a, b) -> a.getTimestamp().isAfter(b.getTimestamp()) ? a : b // Keep latest
                    )
                ));
    }

    /**
     * Calculates a comprehensive score for a model based on multiple performance metrics.
     * The score is a weighted combination of price, throughput, latency, and success rate.
     *
     * @param model the model to score
     * @param metrics the provider metrics for scoring
     * @return ModelScore containing the model and its calculated score
     */
    private ModelScore calculateModelScore(Model model, Map<ProviderMetric.MetricType, ProviderMetric> metrics) {
        double score = 0.0;

        // Price score (lower price = higher score, normalized to 0-1 range)
        double priceScore = calculatePriceScore(model);
        score += PRICE_WEIGHT * priceScore;

        // Performance metrics scores
        if (metrics != null) {
            double throughputScore = calculateThroughputScore(metrics);
            double latencyScore = calculateLatencyScore(metrics);
            double successRateScore = calculateSuccessRateScore(metrics);

            score += THROUGHPUT_WEIGHT * throughputScore;
            score += LATENCY_WEIGHT * latencyScore;
            score += SUCCESS_RATE_WEIGHT * successRateScore;

            log.info("AutoRoutingStrategy.calculateModelScore() - Model {} score breakdown: price={:.3f}, throughput={:.3f}, latency={:.3f}, success={:.3f}, total={:.3f}",
                      model.getModelId(), priceScore, throughputScore, latencyScore, successRateScore, score);
        } else {
            log.info("AutoRoutingStrategy.calculateModelScore() - Model {} has no metrics, price score only: {:.3f}",
                      model.getModelId(), priceScore);
        }

        return new ModelScore(model, score);
    }

    /**
     * Calculates price score where lower cost results in higher score.
     */
    private double calculatePriceScore(Model model) {
        BigDecimal totalCost = model.getInputPricing().add(model.getOutputPricing());
        // Normalize assuming max reasonable cost of 0.01 per token pair
        return Math.max(0.0, Math.min(1.0, 1.0 - totalCost.doubleValue() / 0.01));
    }

    /**
     * Calculates throughput score where higher throughput results in higher score.
     */
    private double calculateThroughputScore(Map<ProviderMetric.MetricType, ProviderMetric> metrics) {
        if (!metrics.containsKey(ProviderMetric.MetricType.THROUGHPUT)) {
            return 0.0;
        }
        double throughput = metrics.get(ProviderMetric.MetricType.THROUGHPUT).getValue().doubleValue();
        // Normalize assuming max reasonable throughput of 1000 requests/minute
        return Math.min(1.0, throughput / 1000.0);
    }

    /**
     * Calculates latency score where lower latency results in higher score.
     */
    private double calculateLatencyScore(Map<ProviderMetric.MetricType, ProviderMetric> metrics) {
        if (!metrics.containsKey(ProviderMetric.MetricType.LATENCY)) {
            return 0.0;
        }
        double latency = metrics.get(ProviderMetric.MetricType.LATENCY).getValue().doubleValue();
        // Normalize assuming max acceptable latency of 5000ms
        return Math.max(0.0, Math.min(1.0, 1.0 - latency / 5000.0));
    }

    /**
     * Calculates success rate score where higher success rate results in higher score.
     */
    private double calculateSuccessRateScore(Map<ProviderMetric.MetricType, ProviderMetric> metrics) {
        if (!metrics.containsKey(ProviderMetric.MetricType.SUCCESS_RATE)) {
            return 0.0;
        }
        double successRate = metrics.get(ProviderMetric.MetricType.SUCCESS_RATE).getValue().doubleValue();
        // Success rate is already 0-1, ensure it's within bounds
        return Math.max(0.0, Math.min(1.0, successRate));
    }

    @Override
    public String getStrategyName() {
        return "AUTO";
    }

    /**
     * Immutable record to hold model scoring information.
     */
    private record ModelScore(Model model, double score) {
        /**
         * Creates a new ModelScore with the given model and score.
         *
         * @param model the model being scored
         * @param score the calculated score
         */
        public ModelScore {
            // Validation in compact constructor
            if (model == null) {
                throw new IllegalArgumentException("Model cannot be null");
            }
            if (score < 0.0) {
                throw new IllegalArgumentException("Score cannot be negative");
            }
        }
    }
}