package com.prismnetai.exception;

/**
 * Exception thrown when there are issues with AI request routing operations.
 * This includes strategy selection failures, provider availability issues, etc.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
public class RoutingException extends RuntimeException {

    /**
     * Constructs a new RoutingException with the specified detail message.
     *
     * @param message the detail message
     */
    public RoutingException(String message) {
        super(message);
    }

    /**
     * Constructs a new RoutingException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}