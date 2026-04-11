package com.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for money transfer between accounts.
 * <p>
 * Both accounts must exist and be active. The source account must have sufficient funds.
 * Transfers to the same account are not allowed.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to transfer money between accounts")
public class TransferRequest {

    @NotNull(message = "From account ID is required")
    @Schema(description = "Source account ID to debit", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID fromAccountId;

    @NotNull(message = "To account ID is required")
    @Schema(description = "Destination account ID to credit", example = "660e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Schema(description = "Amount to transfer (must be positive)", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Description/purpose of the transfer", example = "Payment for services", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}
