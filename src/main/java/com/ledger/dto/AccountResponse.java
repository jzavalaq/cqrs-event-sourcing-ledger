package com.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for account information.
 * <p>
 * Contains all account details including balance, status, and metadata.
 * Used for both single account queries and list responses.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account information response")
public class AccountResponse {

    @Schema(description = "Unique account identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Unique account number", example = "ACC123456789")
    private String accountNumber;

    @Schema(description = "Account holder name", example = "John Doe")
    private String accountHolder;

    @Schema(description = "Current account balance", example = "1500.00")
    private BigDecimal balance;

    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    private String currency;

    @Schema(description = "Account status", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED", "FROZEN"})
    private String status;

    @Schema(description = "Total number of transactions on this account", example = "42")
    private Integer transactionCount;

    @Schema(description = "Account creation timestamp", example = "2024-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-20T14:45:30Z")
    private Instant updatedAt;

    @Schema(description = "Version for optimistic locking", example = "5")
    private Long version;
}
