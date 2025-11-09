package com.prismnetai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

    private String routingStrategy;
    private String routingRuleId;
    private List<ChatMessage> messages;
    private Integer maxTokens = 100;
    private BigDecimal temperature = BigDecimal.valueOf(1.0);
    private Boolean stream = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}