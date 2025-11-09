package com.prismnetai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ChatCompletionResponse {

    private String id;
    private String object;
    private Integer created;
    private String model;
    private RoutingInfo routingInfo;
    private List<ChatChoice> choices;
    private Usage usage;

    @Data
    @Builder
    public static class RoutingInfo {
        private String strategy;
        private String provider;
        private BigDecimal costSavings;
        private Long latencyMs;
    }

    @Data
    @Builder
    public static class ChatChoice {
        private Integer index;
        private ChatMessage message;
        private String finishReason;
    }

    @Data
    @Builder
    public static class ChatMessage {
        private String role;
        private String content;
    }

    @Data
    @Builder
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private BigDecimal cost;
    }
}