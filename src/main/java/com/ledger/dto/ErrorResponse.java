package com.ledger.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Standard error response DTO for API errors.
 * <p>
 * Provides a consistent error format across all API endpoints.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "Error message", example = "Account not found")
    private String error;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Field-level validation errors (only for validation errors)")
    private Map<String, String> fieldErrors;
}
