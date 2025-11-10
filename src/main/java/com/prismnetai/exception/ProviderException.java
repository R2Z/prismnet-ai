package com.prismnetai.exception;

/**
 * Exception thrown when there are issues with AI provider operations.
 * This includes API call failures, authentication issues, rate limiting, etc.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
public class ProviderException extends RuntimeException {

    /**
     * Constructs a new ProviderException with the specified detail message.
     *
     * @param message the detail message
     */
    public ProviderException(String message) {
        super(message);
    }

    /**
     * Constructs a new ProviderException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}