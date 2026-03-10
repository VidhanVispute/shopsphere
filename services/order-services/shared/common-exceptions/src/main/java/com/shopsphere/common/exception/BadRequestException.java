package com.shopsphere.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the request is structurally valid but logically wrong.
 * Examples: trying to order a product with zero stock,
 *           applying an expired coupon, registering with an existing email.
 *
 * Different from a validation error (which comes from @Valid on the DTO).
 * This is a business rule violation caught in the service layer.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}