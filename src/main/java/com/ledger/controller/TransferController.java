package com.ledger.controller;

import com.ledger.dto.TransferRequest;
import com.ledger.dto.TransferResponse;
import com.ledger.service.AccountCommandService;
import com.ledger.util.ApplicationConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Money Transfer operations.
 * <p>
 * Handles all transfer-related commands in the CQRS architecture.
 * Transfers are atomic operations that debit one account and credit another.
 * </p>
 *
 * @see AccountCommandService
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transfer", description = "Money transfer operations")
public class TransferController {

    private final AccountCommandService commandService;

    /**
     * Transfers money between two accounts.
     * <p>
     * This is an atomic operation that will:
     * <ol>
     *   <li>Validate both accounts exist and are active</li>
     *   <li>Check sufficient funds in source account</li>
     *   <li>Debit the source account</li>
     *   <li>Credit the destination account</li>
     *   <li>Record ledger entries for both accounts</li>
     *   <li>Emit a MoneyTransferredEvent</li>
     * </ol>
     * </p>
     *
     * @param request the transfer request containing source, destination, amount, and description
     * @return transfer response with status and details
     */
    @Operation(summary = "Transfer money", description = "Transfers funds from one account to another atomically")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transfer completed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransferResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request, insufficient funds, or same account transfer",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Source or destination account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<TransferResponse> transferMoney(@Valid @RequestBody TransferRequest request) {
        log.info("Processing transfer from {} to {} amount {}",
            request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        commandService.transferMoney(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.builder()
            .transferId(UUID.randomUUID())
            .fromAccountId(request.getFromAccountId())
            .toAccountId(request.getToAccountId())
            .amount(request.getAmount())
            .status(ApplicationConstants.TRANSFER_STATUS_COMPLETED)
            .message("Transfer completed successfully")
            .build());
    }
}
