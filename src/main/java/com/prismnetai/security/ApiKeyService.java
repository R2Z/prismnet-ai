package com.prismnetai.security;

import java.util.Optional;

/**
 * Service to authenticate presented API keys and expose associated metadata like clientId.
 */
public interface ApiKeyService {

    /**
     * Validate the presented token (format expected: "<kid>.<secret>") and return the associated ApiKeyRecord
     * if valid. Returns Optional.empty() on invalid/unknown keys.
     */
    Optional<ApiKeyRecord> authenticate(String presentedToken);

    /**
     * Create a new API key for the given clientId. Returns a result containing the key id and the
     * raw token (kid.secret) which MUST be shown to the user exactly once.
     */
    ApiKeyCreateResult createKey(String clientId, String description);
}
