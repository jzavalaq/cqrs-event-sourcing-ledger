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

/**
 * Request DTO for credit (deposit) or debit (withdrawal) operations.
 * <p>
 * Used for single-account balance adjustments. For transfers between accounts,
 * use {@link TransferRequest} instead.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to credit or debit an account")
public class CreditDebitRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Schema(description = "Amount to credit or debit (must be positive)", example = "250.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Description of the transaction", example = "Cash deposit", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}
