package com.shopsphere.common.exception;

import com.shopsphere.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handling for ALL services that import common-exceptions.
 *
 * WHY @RestControllerAdvice:
 * Without this, an unhandled exception returns a Spring Boot default error page
 * with HTML or a raw stack trace — neither of which your frontend can parse.
 * This class intercepts every exception BEFORE it reaches the client
 * and wraps it in a consistent ApiResponse shape.
 *
 * HOW it works:
 * Spring scans for @ExceptionHandler methods in this class.
 * When an exception is thrown anywhere in the service,
 * Spring checks if this class handles that exception type.
 * If yes, it calls the matching handler method instead of crashing.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles @Valid annotation failures on request DTOs.
     * When a field fails validation (e.g. @NotBlank, @Email),
     * Spring throws MethodArgumentNotValidException.
     * We extract all field errors and return them as a map.
     *
     * Response shape:
     * {
     *   "success": false,
     *   "message": "Validation failed",
     *   "data": { "email": "must not be blank", "password": "size must be between 8 and 50" }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    // Catches anything not explicitly handled above
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
        // NOTE: We intentionally don't expose ex.getMessage() here.
        // Internal error details should never reach the client in production.
    }
}