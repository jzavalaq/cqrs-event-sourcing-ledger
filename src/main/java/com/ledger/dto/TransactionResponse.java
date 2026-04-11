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
 * Response DTO for transaction/ledger entry information.
 * <p>
 * Represents a single transaction record in the account's ledger.
 * Each entry shows the type, amount, and resulting balance.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction/ledger entry response")
public class TransactionResponse {

    @Schema(description = "Unique transaction entry ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Account ID this transaction belongs to", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID accountId;

    @Schema(description = "Transaction type", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT", "TRANSFER_IN", "TRANSFER_OUT"})
    private String type;

    @Schema(description = "Transaction amount", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Account balance after this transaction", example = "1500.00")
    private BigDecimal balanceAfter;

    @Schema(description = "Transaction description", example = "Transfer from ACC123456789")
    private String description;

    @Schema(description = "Related account ID for transfers", example = "660e8400-e29b-41d4-a716-446655440000")
    private UUID relatedAccountId;

    @Schema(description = "Transaction timestamp", example = "2024-01-20T14:45:30Z")
    private Instant createdAt;

    @Schema(description = "Version for optimistic locking", example = "1")
    private Long version;
}
