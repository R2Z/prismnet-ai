package com.prismnetai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * In-memory implementation that parses configuration entries and authenticates tokens.
 *
 * Configuration format (comma-separated list) for property/env KINGSTON_API_KEYS:
 *   kid:clientId:secret,kid2:client2:secret2
 *
 * Presented token format:
 *   kid.secret  (dot-separated)
 */
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyServiceImpl.class);

    private final Map<String, ApiKeyRecord> keysById = new java.util.concurrent.ConcurrentHashMap<>();

    public ApiKeyServiceImpl(String rawConfig) {
        String source = rawConfig == null ? "" : rawConfig.trim();
        if (source.isEmpty()) {
            log.warn("No KINGSTON_API_KEYS configured - falling back to a single default key (not for production)");
            // default to a single key usable for local/dev; id "default" and clientId "anonymous"
            String defaultSecret = "sk-secret-key";
            String kid = "default";
            keysById.put(kid, new ApiKeyRecord(kid, "anonymous", sha256Hex(defaultSecret), Instant.now()));
            return;
        }

        String[] entries = source.split(",");
        for (String entry : entries) {
            String e = entry.trim();
            if (e.isEmpty()) continue;
            // expected kid:clientId:secret
            String[] parts = e.split(":", 3);
            if (parts.length != 3) {
                log.warn("Skipping invalid api-key entry: {} (expected kid:clientId:secret)", e);
                continue;
            }
            String kid = parts[0].trim();
            String clientId = parts[1].trim();
            String secret = parts[2];
            if (!StringUtils.hasText(kid) || !StringUtils.hasText(clientId) || !StringUtils.hasText(secret)) {
                log.warn("Skipping incomplete api-key entry: {}", e);
                continue;
            }
            String hashed = sha256Hex(secret);
            ApiKeyRecord rec = new ApiKeyRecord(kid, clientId, hashed, Instant.now());
            keysById.put(kid, rec);
            log.info("Loaded API key entry kid={} clientId={}", kid, clientId);
        }
    }

    @Override
    public Optional<ApiKeyRecord> authenticate(String presentedToken) {
        if (!StringUtils.hasText(presentedToken)) return Optional.empty();
        // expected format kid.secret
        int dot = presentedToken.indexOf('.');
        if (dot <= 0 || dot == presentedToken.length() - 1) return Optional.empty();
        String kid = presentedToken.substring(0, dot);
        String secret = presentedToken.substring(dot + 1);
        ApiKeyRecord rec = keysById.get(kid);
        if (rec == null) return Optional.empty();
        String presentedHash = sha256Hex(secret);
        if (constantTimeEquals(rec.getHashedSecret(), presentedHash)) {
            return Optional.of(rec);
        }
        return Optional.empty();
    }

    @Override
    public ApiKeyCreateResult createKey(String clientId, String description) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }
        // generate kid and secret
        String kid = "k" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);
        byte[] secretBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(secretBytes);
        String secret = bytesToHex(secretBytes);

        String hashed = sha256Hex(secret);
        ApiKeyRecord rec = new ApiKeyRecord(kid, clientId, hashed, Instant.now());
        keysById.put(kid, rec);
        String token = kid + "." + secret;
        log.info("Created new API key kid={} for client={}", kid, clientId);
        return new ApiKeyCreateResult(kid, token, rec.getCreatedAt(), clientId, description);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] A = a == null ? new byte[0] : a.getBytes(StandardCharsets.UTF_8);
        byte[] B = b == null ? new byte[0] : b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(A, B);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
