package com.ecommerce.ecommerce.exception;

import java.time.Instant;
import java.util.Map;

public class ApiError {

    private final Instant timestamp;
    private final int status;
    private final String message;
    private final Map<String, String> validationErrors;

    public ApiError(int status, String message) {
        this(status, message, Map.of());
    }

    public ApiError(int status, String message, Map<String, String> validationErrors) {
        this.timestamp = Instant.now();
        this.status = status;
        this.message = message;
        this.validationErrors = validationErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
