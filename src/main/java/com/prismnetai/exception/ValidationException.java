package com.prismnetai.exception;

import java.util.Map;

/**
 * Exception thrown when request validation fails.
 * Contains detailed validation error information for better error reporting.
 *
 * @author PrismNet AI Team
 * @version 1.0
 * @since 1.0
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> validationErrors;

    /**
     * Constructs a new ValidationException with the specified detail message and validation errors.
     *
     * @param message the detail message
     * @param validationErrors map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = Map.copyOf(validationErrors);
    }

    /**
     * Constructs a new ValidationException with the specified detail message, validation errors, and cause.
     *
     * @param message the detail message
     * @param validationErrors map of field names to error messages
     * @param cause the cause of this exception
     */
    public ValidationException(String message, Map<String, String> validationErrors, Throwable cause) {
        super(message, cause);
        this.validationErrors = Map.copyOf(validationErrors);
    }

    /**
     * Returns the validation errors.
     *
     * @return an unmodifiable map of field names to error messages
     */
    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}