package com.prismnetai.controller;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;
import com.prismnetai.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/v1/chat/completions")
@RequiredArgsConstructor
@Tag(name = "Chat Completions", description = "AI chat completion endpoints with intelligent routing")
public class ChatCompletionController {

    private final RoutingService routingService;

    @PostMapping
    @Operation(summary = "Create chat completion with routing",
               description = "Submit a chat completion request that will be routed based on the specified strategy")
    public ResponseEntity<ChatCompletionResponse> createCompletion(
            @RequestBody ChatCompletionRequest request,
            Authentication authentication) {

        log.info("ChatCompletionController.createCompletion() - Received chat completion request with routing strategy: {}, messageCount: {}",
                 request.getRoutingStrategy(), request.getMessages() != null ? request.getMessages().size() : 0);

        // Extract user ID from authentication
        String userId = authentication.getName();
        log.debug("ChatCompletionController.createCompletion() - Processing request for authenticated user: {}", userId);

        // Validate request parameters
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            log.warn("ChatCompletionController.createCompletion() - Request validation failed: no messages provided");
            throw new IllegalArgumentException("Messages are required");
        }

        if (request.getRoutingStrategy() == null || request.getRoutingStrategy().trim().isEmpty()) {
            log.warn("ChatCompletionController.createCompletion() - Request validation failed: no routing strategy provided");
            throw new IllegalArgumentException("Routing strategy is required");
        }

        String prompt = extractPrompt(request);
        log.debug("ChatCompletionController.createCompletion() - Extracted prompt length: {} characters", prompt.length());

        // Route the request
        log.info("ChatCompletionController.createCompletion() - Routing request for user: {} with strategy: {}",
                 userId, request.getRoutingStrategy());

        AiRequest aiRequest = routingService.routeRequest(
            userId,
            AiRequest.RoutingStrategy.valueOf(request.getRoutingStrategy().toUpperCase()),
            prompt,
            request.getMaxTokens()
        );

        log.info("ChatCompletionController.createCompletion() - Request routed successfully, requestId: {}, selectedModel: {}, selectedProvider: {}",
                 aiRequest.getId(), aiRequest.getSelectedModel().getModelId(), aiRequest.getSelectedProvider().getName());

        // For now, return a placeholder response
        // In a full implementation, this would call the actual AI provider
        ChatCompletionResponse response = ChatCompletionResponse.builder()
            .id("chatcmpl-" + aiRequest.getId())
            .object("chat.completion")
            .created((int)(System.currentTimeMillis() / 1000))
            .model(aiRequest.getSelectedModel().getModelId())
            .routingInfo(ChatCompletionResponse.RoutingInfo.builder()
                .strategy(request.getRoutingStrategy())
                .provider(aiRequest.getSelectedProvider().getName())
                .costSavings(BigDecimal.valueOf(0.0)) // Would be calculated based on routing
                .latencyMs(0L)    // Would be measured
                .build())
            .choices(java.util.List.of(
                ChatCompletionResponse.ChatChoice.builder()
                    .index(0)
                    .message(ChatCompletionResponse.ChatMessage.builder()
                        .role("assistant")
                        .content("This is a placeholder response. Price routing to " +
                                aiRequest.getSelectedProvider().getName() + " successful.")
                        .build())
                    .finishReason("stop")
                    .build()
            ))
            .usage(ChatCompletionResponse.Usage.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .cost(BigDecimal.valueOf(0.001))
                .build())
            .build();

        log.info("ChatCompletionController.createCompletion() - Successfully processed request for user: {}, returning response with id: {}",
                 userId, response.getId());

        return ResponseEntity.ok(response);
    }

    private String extractPrompt(ChatCompletionRequest request) {
        log.debug("ChatCompletionController.extractPrompt() - Extracting user message from {} messages",
                  request.getMessages().size());

        String prompt = request.getMessages().stream()
            .filter(msg -> "user".equals(msg.getRole()))
            .findFirst()
            .map(ChatCompletionRequest.ChatMessage::getContent)
            .orElse("No user message found");

        log.debug("ChatCompletionController.extractPrompt() - Extracted prompt: {}...",
                  prompt.substring(0, Math.min(50, prompt.length())));

        return prompt;
    }
}