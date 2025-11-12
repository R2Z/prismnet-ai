package com.prismnetai.security;

import com.prismnetai.entity.ApiKey;
import com.prismnetai.entity.Client;
import com.prismnetai.repository.ApiKeyRepository;
import com.prismnetai.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JpaApiKeyService implements ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(JpaApiKeyService.class);

    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;

    public JpaApiKeyService(ApiKeyRepository apiKeyRepository, ClientRepository clientRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiKeyRecord> authenticate(String presentedToken) {
        if (!StringUtils.hasText(presentedToken)) return Optional.empty();
        int dot = presentedToken.indexOf('.');
        if (dot <= 0 || dot == presentedToken.length() - 1) return Optional.empty();
        String kid = presentedToken.substring(0, dot);
        String secret = presentedToken.substring(dot + 1);
        Optional<ApiKey> oe = apiKeyRepository.findById(kid);
        if (oe.isEmpty()) return Optional.empty();
        ApiKey e = oe.get();
        if (e.isRevoked()) return Optional.empty();
    String presentedHash = ApiKeyUtils.sha256Hex(secret);
        if (!constantTimeEquals(e.getHashedSecret(), presentedHash)) return Optional.empty();
        // update last used
        e.setLastUsed(Instant.now());
        apiKeyRepository.save(e);
        Client c = e.getClient();
        return Optional.of(new ApiKeyRecord(e.getId(), c.getClientId(), e.getHashedSecret(), e.getCreatedAt()));
    }

    @Override
    @Transactional
    public ApiKeyCreateResult createKey(String clientId, String description) {
        if (clientId == null || clientId.isBlank()) throw new IllegalArgumentException("clientId required");
        Client client = clientRepository.findById(clientId).orElseGet(() -> {
            Client ce = new Client(clientId, clientId, null);
            return clientRepository.save(ce);
        });

    String kid = "k" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);
    byte[] secretBytes = new byte[32];
    new java.security.SecureRandom().nextBytes(secretBytes);
    String secret = ApiKeyUtils.bytesToHex(secretBytes);
    String hashed = ApiKeyUtils.sha256Hex(secret);

        ApiKey key = new ApiKey(kid, client, hashed, description, null);
        apiKeyRepository.save(key);

        String token = kid + "." + secret;
        log.info("Created API key {} for client {}", kid, clientId);
        return new ApiKeyCreateResult(kid, token, key.getCreatedAt(), clientId, description);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] A = a == null ? new byte[0] : a.getBytes(StandardCharsets.UTF_8);
        byte[] B = b == null ? new byte[0] : b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(A, B);
    }
}
