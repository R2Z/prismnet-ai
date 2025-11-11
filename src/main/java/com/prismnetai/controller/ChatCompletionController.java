package com.prismnetai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prismnetai.dto.ChatCompletionRequest;
import com.prismnetai.dto.ChatCompletionResponse;
import com.prismnetai.entity.AiRequest;
import com.prismnetai.service.RoutingService;
import com.prismnetai.service.provider.ProviderServiceRegistry;
import com.prismnetai.validation.ChatCompletionRequestValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/chat/completions")
@RequiredArgsConstructor
@Tag(name = "Chat Completions", description = "AI chat completion endpoints with intelligent routing")
public class ChatCompletionController {

    private final RoutingService routingService;
    private final ProviderServiceRegistry providerServiceRegistry;
    private final ChatCompletionRequestValidator validator;

    @PostMapping
    @Operation(summary = "Create chat completion with routing",
                description = "Submit a chat completion request that will be routed based on the specified strategy")
    public ResponseEntity<ChatCompletionResponse> createCompletion(
            @RequestBody ChatCompletionRequest request,
            Authentication authentication) {

        log.info("ChatCompletionController.createCompletion() - Received chat completion request with routing strategy: {}, messageCount: {}",
                  request.getRoutingStrategy(), request.getMessages() != null ? request.getMessages().size() : 0);

        // Extract user ID from authentication (use anonymous for demo if not authenticated)
        String userId = (authentication != null && authentication.getName() != null) ? authentication.getName() : "anonymous-demo-user";
        log.info("ChatCompletionController.createCompletion() - Processing request for user: {}", userId);

        // Validate request
        validator.validate(request);

        String prompt = extractPrompt(request);
        log.info("ChatCompletionController.createCompletion() - Extracted prompt length: {} characters", prompt.length());

        // Route the request
        log.info("ChatCompletionController.createCompletion() - Routing request for user: {} with strategy: {}",
                  userId, request.getRoutingStrategy());

        AiRequest aiRequest = routingService.routeRequest(
            userId,
            AiRequest.RoutingStrategy.valueOf(request.getRoutingStrategy().toUpperCase()),
            prompt,
            request.getMaxTokens(),
            request.getPreferredModel()
        );

        log.info("ChatCompletionController.createCompletion() - Request routed successfully, requestId: {}, selectedModel: {}, selectedProvider: {}",
                  aiRequest.getId(), aiRequest.getSelectedModel().getModelId(), aiRequest.getSelectedProvider().getName());

        // Get the appropriate provider service and call the completion
        var providerService = providerServiceRegistry.getProviderService(aiRequest.getSelectedProvider().getName());
        ChatCompletionResponse response = providerService.callCompletion(request, aiRequest);

        log.info("ChatCompletionController.createCompletion() - Successfully processed request for user: {}, returning response",
                  userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the user prompt from the chat completion request.
     * This method finds the first user message and returns its content.
     *
     * @param request the chat completion request
     * @return the extracted prompt, or "No user message found" if none exists
     */
    private String extractPrompt(ChatCompletionRequest request) {
        log.info("ChatCompletionController.extractPrompt() - Extracting user message from {} messages",
                  request.getMessages().size());

        String prompt = request.getMessages().stream()
            .filter(msg -> "user".equals(msg.getRole()))
            .findFirst()
            .map(ChatCompletionRequest.ChatMessage::getContent)
            .orElse("No user message found");

        log.info("ChatCompletionController.extractPrompt() - Extracted prompt: {}...",
                  prompt.substring(0, Math.min(50, prompt.length())));

        return prompt;
    }
}