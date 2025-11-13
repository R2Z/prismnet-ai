package com.prismnetai.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

    // New flexible routing fields
    private String model;           // Optional single model (direct routing)
    private List<String> models;    // Optional multiple models (fallback routing)
    private ProviderOptions provider; // Optional provider configuration

    // Existing fields
    private List<ChatMessage> messages;
    private Integer maxTokens = 100;
    private BigDecimal temperature = BigDecimal.valueOf(1.0);
    private Boolean stream = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderOptions {
        private String sort;           // "throughput", "latency", "price", etc.
        private List<String> order;    // Provider priority order
        private Boolean allowFallbacks; // Whether to allow fallbacks
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}