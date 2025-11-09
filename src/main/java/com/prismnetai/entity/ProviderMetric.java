package com.prismnetai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "provider_metric")
public class ProviderMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public enum MetricType {
        LATENCY, THROUGHPUT, ERROR_RATE, SUCCESS_RATE
    }
}