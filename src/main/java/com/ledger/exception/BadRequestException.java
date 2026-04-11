package com.ledger.exception;

/**
 * Exception thrown when a request contains invalid data or parameters.
 * <p>
 * This exception results in an HTTP 400 Bad Request response.
 * </p>
 */
public class BadRequestException extends RuntimeException {

    /**
     * Constructs a new BadRequestException with the specified message.
     *
     * @param message the detail message
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Constructs a new BadRequestException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a BadRequestException for an invalid parameter.
     *
     * @param parameter the name of the invalid parameter
     * @param reason the reason why the parameter is invalid
     * @return a new BadRequestException with appropriate message
     */
    public static BadRequestException forInvalidParameter(String parameter, String reason) {
        return new BadRequestException(
            String.format("Invalid parameter '%s': %s", parameter, reason)
        );
    }
}
