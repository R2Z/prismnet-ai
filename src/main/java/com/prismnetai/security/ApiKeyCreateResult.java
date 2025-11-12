package com.prismnetai.security;

import java.time.Instant;

/**
 * Result returned when a new API key is created. token contains the raw token (kid.secret)
 * which must be shown to the caller exactly once.
 */
public class ApiKeyCreateResult {
    private final String id;
    private final String token; // kid.secret
    private final Instant createdAt;
    private final String clientId;
    private final String description;

    public ApiKeyCreateResult(String id, String token, Instant createdAt, String clientId, String description) {
        this.id = id;
        this.token = token;
        this.createdAt = createdAt;
        this.clientId = clientId;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getClientId() {
        return clientId;
    }

    public String getDescription() {
        return description;
    }
}
