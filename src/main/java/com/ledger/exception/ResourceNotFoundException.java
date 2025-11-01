package com.ledger.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException accountNotFound(UUID accountId) {
        return new ResourceNotFoundException("Account not found with id: " + accountId);
    }
}
