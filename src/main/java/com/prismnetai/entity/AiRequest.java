package com.prismnetai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_request")
@EntityListeners(AuditingEntityListener.class)
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_strategy", nullable = false)
    private RoutingStrategy routingStrategy;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(precision = 3, scale = 2)
    private BigDecimal temperature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_provider_id")
    private Provider selectedProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_model_id")
    private Model selectedModel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(precision = 10, scale = 6)
    private BigDecimal cost;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    public enum RoutingStrategy {
        PRICE, THROUGHPUT, LATENCY, CUSTOM_ORDER, AUTO
    }

    public enum RequestStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}