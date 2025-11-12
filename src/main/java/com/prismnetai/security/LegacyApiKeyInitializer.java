package com.prismnetai.security;

import com.prismnetai.entity.ApiKey;
import com.prismnetai.entity.Client;
import com.prismnetai.repository.ApiKeyRepository;
import com.prismnetai.repository.ClientRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

 

/**
 * On startup, seed any API keys that were provided via KINGSTON_API_KEYS (legacy format kid:clientId:secret)
 * into the database if they don't already exist. This allows easy bootstrapping from env vars.
 */
@Component
public class LegacyApiKeyInitializer {

    private static final Logger log = LoggerFactory.getLogger(LegacyApiKeyInitializer.class);

    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;

    public LegacyApiKeyInitializer(ApiKeyRepository apiKeyRepository, ClientRepository clientRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientRepository = clientRepository;
    }

    @PostConstruct
    public void init() {
        String fromEnv = System.getenv("KINGSTON_API_KEYS");
        if (fromEnv == null || fromEnv.isBlank()) return;
        String[] entries = fromEnv.split(",");
        for (String entry : entries) {
            String e = entry.trim();
            if (e.isEmpty()) continue;
            String[] parts = e.split(":", 3);
            if (parts.length != 3) {
                log.warn("Ignoring malformed legacy API key entry: {}", e);
                continue;
            }
            String kid = parts[0].trim();
            String clientId = parts[1].trim();
            String secret = parts[2];

            if (apiKeyRepository.existsById(kid)) {
                log.info("Legacy key {} already exists in DB, skipping", kid);
                continue;
            }

            Client client = clientRepository.findById(clientId).orElseGet(() -> clientRepository.save(new Client(clientId, clientId, null)));

            // store hashed secret
            String hashed = com.prismnetai.security.ApiKeyUtils.sha256Hex(secret);
            ApiKey entity = new ApiKey(kid, client, hashed, "legacy-init", null);
            apiKeyRepository.save(entity);
            log.info("Seeded legacy API key {} for client {}", kid, clientId);
        }
    }
}
