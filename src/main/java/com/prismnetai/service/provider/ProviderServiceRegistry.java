package com.prismnetai.service.provider;

import java.util.List;

import org.springframework.stereotype.Component;

import com.prismnetai.exception.ProviderException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry for managing AI provider services. This class provides a centralized
 * way to find and access provider services based on provider names.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderServiceRegistry {

    private final List<AiProviderService> providerServices;

    // Thread-safe access to provider services list
    private volatile List<AiProviderService> cachedServices = null;

    /**
     * Finds the appropriate provider service for the given provider name.
     *
     * @param providerName the name of the provider
     * @return the provider service that can handle the provider
     * @throws IllegalArgumentException if providerName is null or empty
     * @throws ProviderException if no suitable provider service is found
     */
    public AiProviderService getProviderService(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }

        log.info("ProviderServiceRegistry.getProviderService() - Looking for service for provider: {}", providerName);

        return providerServices.stream()
            .filter(service -> {
                try {
                    return service.canHandle(providerName);
                } catch (Exception e) {
                    log.warn("ProviderServiceRegistry.getProviderService() - Error checking if service can handle provider {}: {}",
                            providerName, e.getMessage());
                    return false;
                }
            })
            .findFirst()
            .orElseThrow(() -> {
                log.error("ProviderServiceRegistry.getProviderService() - No provider service found for provider: {}", providerName);
                return new com.prismnetai.exception.ProviderException("No provider service available for provider: " + providerName);
            });
    }

    /**
     * Returns all registered provider services.
     * This method provides thread-safe access to the provider services list.
     *
     * @return immutable list of all provider services
     */
    public List<AiProviderService> getAllProviderServices() {
        // Double-checked locking for thread-safe lazy initialization
        List<AiProviderService> services = cachedServices;
        if (services == null) {
            synchronized (this) {
                services = cachedServices;
                if (services == null) {
                    cachedServices = services = List.copyOf(providerServices);
                }
            }
        }
        return services;
    }
}