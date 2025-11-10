package com.prismnetai.service.routing;

import java.time.Duration;
import java.time.LocalDateTime;
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
@Component("THROUGHPUT")
@RequiredArgsConstructor
public class ThroughputRoutingStrategy implements RoutingStrategy {

    private final ModelRepository modelRepository;
    private final ProviderMetricRepository providerMetricRepository;
    private final Duration metricsLookbackDuration;

    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders, String userId) {
        log.info("ThroughputRoutingStrategy.selectModel() - Starting throughput-based model selection");

        if (availableProviders == null) {
            log.warn("ThroughputRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        if (availableProviders.isEmpty()) {
            log.warn("ThroughputRoutingStrategy.selectModel() - Empty providers list provided");
            return Optional.empty();
        }

        log.info("ThroughputRoutingStrategy.selectModel() - Evaluating {} available providers: {}",
                  availableProviders.size(),
                  availableProviders.stream().map(Provider::getName).toList());

        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();
        // Get latest throughput metrics for available providers
        LocalDateTime since = LocalDateTime.now().minus(metricsLookbackDuration);
        List<ProviderMetric> throughputMetrics = providerMetricRepository
                .findRecentMetricsByProvidersAndType(providerIds, ProviderMetric.MetricType.THROUGHPUT, since);

        log.info("ThroughputRoutingStrategy.selectModel() - Found {} throughput metrics",
                  throughputMetrics.size());

        // Group metrics by provider and get the latest value for each
        Map<Long, ProviderMetric> latestThroughputByProvider = throughputMetrics.stream()
                .collect(Collectors.groupingBy(
                    pm -> pm.getProvider().getId(),
                    Collectors.collectingAndThen(
                        Collectors.maxBy((a, b) -> a.getTimestamp().compareTo(b.getTimestamp())),
                        opt -> opt.orElse(null)
                    )
                ));

        // Get active models and sort by throughput (highest first)
        List<Model> activeModels = modelRepository.findActiveModelsByProviderIds(providerIds);

        Optional<Model> selectedModel = activeModels.stream()
                .filter(model -> {
                    boolean isAvailable = providerIds.contains(model.getProvider().getId());
                    ProviderMetric throughputMetric = latestThroughputByProvider.get(model.getProvider().getId());
                    boolean hasThroughputData = throughputMetric != null;
                    log.info("ThroughputRoutingStrategy.selectModel() - Model {} from provider {} is available: {}, has throughput data: {}",
                              model.getModelId(), model.getProvider().getName(), isAvailable, hasThroughputData);
                    return isAvailable && hasThroughputData;
                })
                .sorted((a, b) -> {
                    ProviderMetric throughputA = latestThroughputByProvider.get(a.getProvider().getId());
                    ProviderMetric throughputB = latestThroughputByProvider.get(b.getProvider().getId());
                    // Sort by throughput descending (higher throughput first)
                    return throughputB.getValue().compareTo(throughputA.getValue());
                })
                .findFirst();

        if (selectedModel.isPresent()) {
            Model model = selectedModel.get();
            ProviderMetric throughputMetric = latestThroughputByProvider.get(model.getProvider().getId());
            log.info("ThroughputRoutingStrategy.selectModel() - Selected model: {} from provider {} with throughput: {}",
                     model.getModelId(), model.getProvider().getName(),
                     throughputMetric != null ? throughputMetric.getValue() : "N/A");
        } else {
            log.warn("ThroughputRoutingStrategy.selectModel() - No suitable model found among available providers with throughput data");
        }

        log.info("ThroughputRoutingStrategy.selectModel() - Completed throughput-based model selection");
        return selectedModel;
    }

    @Override
    public String getStrategyName() {
        return "THROUGHPUT";
    }
}