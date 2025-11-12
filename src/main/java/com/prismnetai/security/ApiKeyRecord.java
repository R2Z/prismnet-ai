package com.prismnetai.security;

import java.time.Instant;

/**
 * Lightweight record representing a stored API key entry.
 * Note: only hashedSecret should be stored for safety.
 */
public class ApiKeyRecord {

    private final String id; // key id (kid)
    private final String clientId; // logical client identifier
    private final String hashedSecret; // hex-encoded SHA-256 of secret
    private final Instant createdAt;

    public ApiKeyRecord(String id, String clientId, String hashedSecret, Instant createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.hashedSecret = hashedSecret;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getHashedSecret() {
        return hashedSecret;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
