package com.shopsphere.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource doesn't exist in the database.
 * Examples: user ID not found, product SKU not found, order not found.
 *
 * @ResponseStatus maps this exception to HTTP 404 automatically
 * when thrown from a controller — but we also handle it explicitly
 * in GlobalExceptionHandler for consistent ApiResponse wrapping.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Convenience constructor: "User not found with id: 42"
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }
}