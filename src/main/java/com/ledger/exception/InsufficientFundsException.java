package com.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Exception thrown when an account has insufficient funds for a debit operation.
 * <p>
 * This exception results in an HTTP 400 Bad Request response.
 * </p>
 */
public class InsufficientFundsException extends RuntimeException {

    /**
     * Constructs a new InsufficientFundsException with the specified message.
     *
     * @param message the detail message
     */
    public InsufficientFundsException(String message) {
        super(message);
    }

    /**
     * Creates an InsufficientFundsException with detailed information about the failed operation.
     *
     * @param accountId the ID of the account with insufficient funds
     * @param requested the amount that was requested
     * @param available the amount that was available
     * @return a new InsufficientFundsException with appropriate message
     */
    public static InsufficientFundsException forAccount(UUID accountId, BigDecimal requested, BigDecimal available) {
        return new InsufficientFundsException(
            String.format("Insufficient funds in account %s. Requested: %s, Available: %s",
                accountId, requested, available)
        );
    }
}
