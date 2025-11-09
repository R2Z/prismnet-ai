package com.prismnetai.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.prismnetai.entity.Provider;
import com.prismnetai.entity.ProviderMetric;

@Repository
public interface ProviderMetricRepository extends JpaRepository<ProviderMetric, Long> {

    List<ProviderMetric> findByProviderAndMetricType(Provider provider, ProviderMetric.MetricType metricType);

    @Query("SELECT pm FROM ProviderMetric pm WHERE pm.provider.id IN :providerIds AND pm.metricType = :metricType AND pm.timestamp >= :since ORDER BY pm.timestamp DESC")
    List<ProviderMetric> findRecentMetricsByProvidersAndType(@Param("providerIds") List<Long> providerIds,
                                                             @Param("metricType") ProviderMetric.MetricType metricType,
                                                             @Param("since") LocalDateTime since);

    @Query("SELECT pm FROM ProviderMetric pm WHERE pm.provider.id = :providerId AND pm.metricType = :metricType ORDER BY pm.timestamp DESC LIMIT 1")
    Optional<ProviderMetric> findLatestMetricByProviderAndType(@Param("providerId") Long providerId,
                                                               @Param("metricType") ProviderMetric.MetricType metricType);

    @Query("SELECT pm FROM ProviderMetric pm WHERE pm.provider.id IN :providerIds AND pm.metricType = :metricType ORDER BY pm.timestamp DESC")
    List<ProviderMetric> findLatestMetricsByProvidersAndType(@Param("providerIds") List<Long> providerIds,
                                                             @Param("metricType") ProviderMetric.MetricType metricType);
}