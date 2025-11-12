package com.prismnetai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "name", nullable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    @Column(name = "configuration", columnDefinition = "json")
    private String configuration;

    public Client() {}

    public Client(String clientId, String name, String configuration) {
        this.clientId = clientId;
        this.name = name;
        this.configuration = configuration;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
