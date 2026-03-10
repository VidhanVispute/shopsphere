package com.shopsphere.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response wrapper for ALL API endpoints across ALL services.
 *
 * WHY a wrapper instead of returning raw objects?
 * Without it, a successful response looks like:   { "id": 1, "name": "Vidhan" }
 * An error looks like:                            { "error": "Not found" }
 * The client has to guess the shape based on HTTP status code.
 *
 * With ApiResponse, EVERY response looks the same:
 * {
 *   "success": true,
 *   "message": "User fetched successfully",
 *   "data": { "id": 1, "name": "Vidhan" }
 * }
 * The client always knows where to look.
 *
 * <T> is a generic type — the data payload varies per endpoint.
 * User endpoint returns ApiResponse<UserDto>
 * Order endpoint returns ApiResponse<OrderDto>
 * List endpoint returns ApiResponse<List<ProductDto>>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    // Static factory methods — cleaner than calling the builder every time

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}