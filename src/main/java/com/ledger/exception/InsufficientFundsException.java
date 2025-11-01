package com.ledger.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public static InsufficientFundsException forAccount(java.util.UUID accountId, java.math.BigDecimal requested, java.math.BigDecimal available) {
        return new InsufficientFundsException(
            String.format("Insufficient funds in account %s. Requested: %s, Available: %s",
                accountId, requested, available)
        );
    }
}
