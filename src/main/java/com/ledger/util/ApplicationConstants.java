package com.ledger.util;

/**
 * Application-wide constants.
 * Centralizes magic strings and numbers used throughout the application.
 */
public final class ApplicationConstants {

    private ApplicationConstants() {
        // Utility class, prevent instantiation
    }

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // Currency
    public static final String DEFAULT_CURRENCY = "USD";

    // API Version
    public static final String API_VERSION = "/api/v1";

    // Transfer status
    public static final String TRANSFER_STATUS_COMPLETED = "COMPLETED";
    public static final String TRANSFER_STATUS_PENDING = "PENDING";
    public static final String TRANSFER_STATUS_FAILED = "FAILED";

    // Account status
    public static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";
    public static final String ACCOUNT_STATUS_CLOSED = "CLOSED";
    public static final String ACCOUNT_STATUS_FROZEN = "FROZEN";

    // Correlation ID header
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_KEY = "correlationId";

    // Error URLs
    public static final String ERROR_BASE_URL = "https://ledger.api/errors";
}
