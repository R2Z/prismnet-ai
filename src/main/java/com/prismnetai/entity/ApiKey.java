package com.prismnetai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id")
    private Client client;

    @Column(name = "hashed_secret", nullable = false, length = 128)
    private String hashedSecret;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "last_used")
    private Instant lastUsed;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    public ApiKey() {}

    public ApiKey(String id, Client client, String hashedSecret, String description, String metadata) {
        this.id = id;
        this.client = client;
        this.hashedSecret = hashedSecret;
        this.description = description;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getHashedSecret() {
        return hashedSecret;
    }

    public void setHashedSecret(String hashedSecret) {
        this.hashedSecret = hashedSecret;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
