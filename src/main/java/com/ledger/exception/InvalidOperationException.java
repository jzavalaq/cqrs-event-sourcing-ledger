package com.ledger.exception;

/**
 * Exception thrown when an operation is not valid for the current state.
 * <p>
 * This includes scenarios like:
 * <ul>
 *   <li>Operating on a closed/frozen account</li>
 *   <li>Duplicate account number</li>
 *   <li>Transferring to the same account</li>
 *   <li>Closing an account with non-zero balance</li>
 * </ul>
 * This exception results in an HTTP 400 Bad Request response.
 * </p>
 */
public class InvalidOperationException extends RuntimeException {

    /**
     * Constructs a new InvalidOperationException with the specified message.
     *
     * @param message the detail message
     */
    public InvalidOperationException(String message) {
        super(message);
    }
}
