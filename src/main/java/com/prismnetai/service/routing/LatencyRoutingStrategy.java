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
@Component("LATENCY")
@RequiredArgsConstructor
public class LatencyRoutingStrategy implements RoutingStrategy {

    private final ModelRepository modelRepository;
    private final ProviderMetricRepository providerMetricRepository;
    private final Duration metricsLookbackDuration;

    @Override
    public Optional<Model> selectModel(List<Provider> availableProviders, String userId, String preferredModel) {
        log.info("LatencyRoutingStrategy.selectModel() - Starting latency-based model selection");

        if (availableProviders == null) {
            log.warn("LatencyRoutingStrategy.selectModel() - No providers available for routing");
            return Optional.empty();
        }

        if (availableProviders.isEmpty()) {
            log.warn("LatencyRoutingStrategy.selectModel() - Empty providers list provided");
            return Optional.empty();
        }

        log.info("LatencyRoutingStrategy.selectModel() - Evaluating {} available providers: {}",
                  availableProviders.size(),
                  availableProviders.stream().map(Provider::getName).toList());

        List<Long> providerIds = availableProviders.stream()
                .map(Provider::getId)
                .toList();

        // Get latest latency metrics for available providers
        LocalDateTime since = LocalDateTime.now().minus(metricsLookbackDuration);
        List<ProviderMetric> latencyMetrics = providerMetricRepository
                .findRecentMetricsByProvidersAndType(providerIds, ProviderMetric.MetricType.LATENCY, since);

        log.info("LatencyRoutingStrategy.selectModel() - Found {} latency metrics",
                  latencyMetrics.size());

        // Group metrics by provider and get the latest value for each
        Map<Long, ProviderMetric> latestLatencyByProvider = latencyMetrics.stream()
                .collect(Collectors.groupingBy(
                    pm -> pm.getProvider().getId(),
                    Collectors.collectingAndThen(
                        Collectors.maxBy((a, b) -> a.getTimestamp().compareTo(b.getTimestamp())),
                        opt -> opt.orElse(null)
                    )
                ));

        // Get active models and sort by latency (lowest first)
        List<Model> activeModels = modelRepository.findActiveModelsByProviderIds(providerIds);

        Optional<Model> selectedModel = activeModels.stream()
                .filter(model -> {
                    boolean isAvailable = providerIds.contains(model.getProvider().getId());
                    ProviderMetric latencyMetric = latestLatencyByProvider.get(model.getProvider().getId());
                    boolean hasLatencyData = latencyMetric != null;
                    log.info("LatencyRoutingStrategy.selectModel() - Model {} from provider {} is available: {}, has latency data: {}",
                              model.getModelId(), model.getProvider().getName(), isAvailable, hasLatencyData);
                    return isAvailable && hasLatencyData;
                })
                .sorted((a, b) -> {
                    ProviderMetric latencyA = latestLatencyByProvider.get(a.getProvider().getId());
                    ProviderMetric latencyB = latestLatencyByProvider.get(b.getProvider().getId());
                    // Sort by latency ascending (lower latency first)
                    return latencyA.getValue().compareTo(latencyB.getValue());
                })
                .findFirst();

        if (selectedModel.isPresent()) {
            Model model = selectedModel.get();
            ProviderMetric latencyMetric = latestLatencyByProvider.get(model.getProvider().getId());
            log.info("LatencyRoutingStrategy.selectModel() - Selected model: {} from provider {} with latency: {}ms",
                     model.getModelId(), model.getProvider().getName(),
                     latencyMetric != null ? latencyMetric.getValue() : "N/A");
        } else {
            log.warn("LatencyRoutingStrategy.selectModel() - No suitable model found among available providers with latency data");
        }

        log.info("LatencyRoutingStrategy.selectModel() - Completed latency-based model selection");
        return selectedModel;
    }

    @Override
    public String getStrategyName() {
        return "LATENCY";
    }
}