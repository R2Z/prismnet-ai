package com.prismnetai.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoutingException.class)
    public ResponseEntity<ErrorResponse> handleRoutingException(RoutingException e) {
        log.error("GlobalExceptionHandler.handleRoutingException() - Routing error occurred: {}", e.getMessage(), e);
        ErrorResponse response = new ErrorResponse("ROUTING_ERROR", e.getMessage());
        log.info("GlobalExceptionHandler.handleRoutingException() - Returning BAD_REQUEST response for routing error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<ErrorResponse> handleProviderException(ProviderException e) {
        log.error("GlobalExceptionHandler.handleProviderException() - Provider error occurred: {}", e.getMessage(), e);
        ErrorResponse response = new ErrorResponse("PROVIDER_ERROR", e.getMessage());
        log.info("GlobalExceptionHandler.handleProviderException() - Returning BAD_GATEWAY response for provider error");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("GlobalExceptionHandler.handleValidationException() - Validation error occurred: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
            log.info("GlobalExceptionHandler.handleValidationException() - Field validation error: {} - {}", fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse("VALIDATION_ERROR", "Validation failed", errors);
        log.info("GlobalExceptionHandler.handleValidationException() - Returning BAD_REQUEST response with {} validation errors", errors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("GlobalExceptionHandler.handleIllegalArgumentException() - Illegal argument provided: {}", e.getMessage());
        ErrorResponse response = new ErrorResponse("INVALID_ARGUMENT", e.getMessage());
        log.info("GlobalExceptionHandler.handleIllegalArgumentException() - Returning BAD_REQUEST response for invalid argument");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException e) {
        log.warn("GlobalExceptionHandler.handleValidationException() - Validation failed: {}", e.getMessage());

        ValidationErrorResponse response = new ValidationErrorResponse("VALIDATION_ERROR", e.getMessage(), e.getValidationErrors());
        log.info("GlobalExceptionHandler.handleValidationException() - Returning BAD_REQUEST response with validation errors");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("GlobalExceptionHandler.handleGenericException() - Unexpected error occurred: {}", e.getMessage(), e);
        ErrorResponse response = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        log.info("GlobalExceptionHandler.handleGenericException() - Returning INTERNAL_SERVER_ERROR response for unexpected error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final LocalDateTime timestamp;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        private final Map<String, String> details;

        public ValidationErrorResponse(String error, String message, Map<String, String> details) {
            super(error, message);
            this.details = details;
        }

        public Map<String, String> getDetails() { return details; }
    }
}