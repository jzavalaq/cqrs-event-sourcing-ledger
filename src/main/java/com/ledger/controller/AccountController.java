package com.ledger.controller;

import com.ledger.domain.Account;
import com.ledger.dto.*;
import com.ledger.service.AccountCommandService;
import com.ledger.service.AccountQueryService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Account operations.
 * <p>
 * Implements CQRS with separate endpoints for commands (writes) and queries (reads).
 * Commands modify state and return the updated resource, while queries are read-only.
 * </p>
 *
 * @see AccountCommandService
 * @see AccountQueryService
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account", description = "Account management operations")
public class AccountController {

    private final AccountCommandService commandService;
    private final AccountQueryService queryService;

    // ==================== COMMANDS (Write Side) ====================

    /**
     * Opens a new bank account.
     *
     * @param request the account opening request
     * @return the created account with HTTP 201 status
     */
    @Operation(summary = "Open a new account", description = "Creates a new bank account with the specified details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Account number already exists",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        log.info("Opening account: {}", request.getAccountNumber());
        Account account = commandService.openAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(queryService.getAccountById(account.getId()));
    }

    /**
     * Credits (deposits) funds to an account.
     *
     * @param id the account ID
     * @param request the credit request containing amount and description
     * @return the updated account with new balance
     */
    @Operation(summary = "Credit account", description = "Adds funds to the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account credited successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid amount or account not active",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accounts/{id}/credit")
    public ResponseEntity<AccountResponse> creditAccount(
            @Parameter(description = "Account ID") @PathVariable UUID id,
            @Valid @RequestBody CreditDebitRequest request) {
        log.info("Crediting account {}: +{}", id, request.getAmount());
        commandService.creditAccount(id, request);
        return ResponseEntity.ok(queryService.getAccountById(id));
    }

    /**
     * Debits (withdraws) funds from an account.
     *
     * @param id the account ID
     * @param request the debit request containing amount and description
     * @return the updated account with new balance
     */
    @Operation(summary = "Debit account", description = "Withdraws funds from the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account debited successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Insufficient funds or account not active",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accounts/{id}/debit")
    public ResponseEntity<AccountResponse> debitAccount(
            @Parameter(description = "Account ID") @PathVariable UUID id,
            @Valid @RequestBody CreditDebitRequest request) {
        log.info("Debiting account {}: -{}", id, request.getAmount());
        commandService.debitAccount(id, request);
        return ResponseEntity.ok(queryService.getAccountById(id));
    }

    /**
     * Closes an account.
     * <p>
     * An account can only be closed if it has a zero balance.
     * </p>
     *
     * @param id the account ID
     * @param reason optional reason for closing the account
     */
    @Operation(summary = "Close account", description = "Closes an account with zero balance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Account closed successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot close account with non-zero balance",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeAccount(
            @Parameter(description = "Account ID") @PathVariable UUID id,
            @Parameter(description = "Reason for closing") @RequestParam(required = false, defaultValue = "Account closed") String reason) {
        log.info("Closing account {}: {}", id, reason);
        commandService.closeAccount(id, reason);
    }

    // ==================== QUERIES (Read Side) ====================

    /**
     * Retrieves all accounts (non-paginated).
     * <p>
     * Note: For large datasets, use the paginated endpoint {@code /accounts/paged}.
     * </p>
     *
     * @return list of all accounts
     */
    @Operation(summary = "Get all accounts", description = "Retrieves all accounts (non-paginated, use /accounts/paged for large datasets)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of accounts retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class)))
    })
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.debug("Fetching all accounts");
        return ResponseEntity.ok(queryService.getAllAccounts());
    }

    /**
     * Retrieves all accounts with pagination.
     *
     * @param page the page number (0-indexed)
     * @param size the page size (max 100)
     * @return paginated list of accounts
     */
    @Operation(summary = "Get all accounts (paginated)", description = "Retrieves all accounts with pagination support")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Paginated list of accounts retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/accounts/paged")
    public ResponseEntity<Page<AccountResponse>> getAllAccountsPaged(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching accounts page {} size {}", page, size);
        return ResponseEntity.ok(queryService.getAllAccountsPaged(page, size));
    }

    /**
     * Retrieves an account by its ID.
     *
     * @param id the account ID
     * @return the account details
     */
    @Operation(summary = "Get account by ID", description = "Retrieves account details by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @Parameter(description = "Account ID") @PathVariable UUID id) {
        log.debug("Fetching account {}", id);
        return ResponseEntity.ok(queryService.getAccountById(id));
    }

    /**
     * Retrieves the current balance of an account.
     *
     * @param id the account ID
     * @return the account with balance information
     */
    @Operation(summary = "Get account balance", description = "Retrieves the current balance of an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/accounts/{id}/balance")
    public ResponseEntity<AccountResponse> getAccountBalance(
            @Parameter(description = "Account ID") @PathVariable UUID id) {
        log.debug("Fetching balance for account {}", id);
        return ResponseEntity.ok(queryService.getAccountBalance(id));
    }

    /**
     * Retrieves transaction history for an account (non-paginated).
     * <p>
     * Note: For large datasets, use the paginated endpoint.
     * </p>
     *
     * @param id the account ID
     * @return list of transactions
     */
    @Operation(summary = "Get transaction history", description = "Retrieves transaction history for an account (non-paginated)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction history retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @Parameter(description = "Account ID") @PathVariable UUID id) {
        log.debug("Fetching transaction history for account {}", id);
        return ResponseEntity.ok(queryService.getTransactionHistory(id));
    }

    /**
     * Retrieves transaction history for an account with pagination.
     *
     * @param id the account ID
     * @param page the page number (0-indexed)
     * @param size the page size (max 100)
     * @return paginated list of transactions
     */
    @Operation(summary = "Get transaction history (paginated)", description = "Retrieves transaction history with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Paginated transaction history retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/accounts/{id}/transactions/paged")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistoryPaged(
            @Parameter(description = "Account ID") @PathVariable UUID id,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching paged transaction history for account {} page {} size {}", id, page, size);
        return ResponseEntity.ok(queryService.getTransactionHistoryPaged(id, page, size));
    }

    /**
     * Retrieves event history for an account (event sourcing audit trail).
     *
     * @param id the account ID
     * @return list of events
     */
    @Operation(summary = "Get account events", description = "Retrieves event sourcing history for an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event history retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/accounts/{id}/events")
    public ResponseEntity<List<EventResponse>> getAccountEvents(
            @Parameter(description = "Account ID") @PathVariable UUID id) {
        log.debug("Fetching event history for account {}", id);
        return ResponseEntity.ok(queryService.getEventHistory(id));
    }

    /**
     * Retrieves all events across all accounts with pagination.
     *
     * @param page the page number (0-indexed)
     * @param size the page size (max 100)
     * @return paginated list of events
     */
    @Operation(summary = "Get all events", description = "Retrieves all events with pagination (audit trail)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Paginated event list retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/events")
    public ResponseEntity<Page<EventResponse>> getAllEvents(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching all events page {} size {}", page, size);
        return ResponseEntity.ok(queryService.getAllEvents(page, size));
    }
}
