package com.ledger.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested resource cannot be found.
 * <p>
 * This exception results in an HTTP 404 Not Found response.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a ResourceNotFoundException for a missing account.
     *
     * @param accountId the ID of the account that was not found
     * @return a new ResourceNotFoundException with appropriate message
     */
    public static ResourceNotFoundException accountNotFound(UUID accountId) {
        return new ResourceNotFoundException("Account not found with id: " + accountId);
    }
}
