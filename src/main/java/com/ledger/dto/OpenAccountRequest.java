package com.ledger.dto;

import com.ledger.util.ApplicationConstants;
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
 * Request DTO for opening a new bank account.
 * <p>
 * All fields except currency are required. The account number must be unique.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to open a new bank account")
public class OpenAccountRequest {

    @NotBlank(message = "Account number is required")
    @Size(min = 5, max = 20, message = "Account number must be between 5 and 20 characters")
    @Schema(description = "Unique account number", example = "ACC123456789", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Schema(description = "Name of the account holder", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountHolder;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance must be non-negative")
    @Schema(description = "Initial deposit amount", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal initialBalance;

    @Builder.Default
    @Schema(description = "Currency code (ISO 4217)", example = "USD", defaultValue = "USD")
    private String currency = ApplicationConstants.DEFAULT_CURRENCY;
}
