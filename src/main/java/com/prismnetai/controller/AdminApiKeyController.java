package com.prismnetai.controller;

import com.prismnetai.security.ApiKeyCreateResult;
import com.prismnetai.security.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Simple admin controller to create API keys.
 *
 * Security: this endpoint expects the caller to be authenticated with an API key whose clientId
 * matches the environment variable PRISMNETAI_ADMIN_CLIENT (defaults to 'admin'). The ApiKeyAuthFilter
 * populates request attribute 'clientId'.
 */
@RestController
@RequestMapping(path = "/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminApiKeyController {

    private final ApiKeyService apiKeyService;
    private final String adminClientId;

    public AdminApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
        String c = System.getenv("PRISMNETAI_ADMIN_CLIENT");
        this.adminClientId = (c != null && !c.isBlank()) ? c : "admin";
    }

    @PostMapping(path = "/api-keys", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createApiKey(@RequestBody CreateApiKeyRequest req, HttpServletRequest httpRequest) {
        Object caller = httpRequest.getAttribute("clientId");
        if (caller == null || !adminClientId.equals(caller.toString())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }

        if (req == null || req.getClientId() == null || req.getClientId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "clientId is required"));
        }

        ApiKeyCreateResult result = apiKeyService.createKey(req.getClientId(), req.getDescription());

        // Return token (kid.secret) — caller must store it securely because it will not be retrievable later
        ApiKeyCreationResponse resp = new ApiKeyCreationResponse(result.getId(), result.getToken(), result.getClientId(), result.getCreatedAt(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    public static class CreateApiKeyRequest {
        private String clientId;
        private String description;

        public CreateApiKeyRequest() {}

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class ApiKeyCreationResponse {
        private final String id;
        private final String token;
        private final String clientId;
        private final Instant createdAt;
        private final String description;

        public ApiKeyCreationResponse(String id, String token, String clientId, Instant createdAt, String description) {
            this.id = id;
            this.token = token;
            this.clientId = clientId;
            this.createdAt = createdAt;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getToken() {
            return token;
        }

        public String getClientId() {
            return clientId;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public String getDescription() {
            return description;
        }
    }
}
