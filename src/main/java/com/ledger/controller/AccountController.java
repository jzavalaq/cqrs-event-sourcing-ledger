package com.ledger.controller;

import com.ledger.domain.Account;
import com.ledger.dto.*;
import com.ledger.service.AccountCommandService;
import com.ledger.service.AccountQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Account operations.
 * Implements CQRS with separate endpoints for commands and queries.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController {

    private final AccountCommandService commandService;
    private final AccountQueryService queryService;

    // ==================== COMMANDS (Write Side) ====================

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        Account account = commandService.openAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(queryService.getAccountById(account.getId()));
    }

    @PostMapping("/accounts/{id}/credit")
    public ResponseEntity<AccountResponse> creditAccount(
            @PathVariable UUID id,
            @Valid @RequestBody CreditDebitRequest request) {
        commandService.creditAccount(id, request);
        return ResponseEntity.ok(queryService.getAccountById(id));
    }

    @PostMapping("/accounts/{id}/debit")
    public ResponseEntity<AccountResponse> debitAccount(
            @PathVariable UUID id,
            @Valid @RequestBody CreditDebitRequest request) {
        commandService.debitAccount(id, request);
        return ResponseEntity.ok(queryService.getAccountById(id));
    }

    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeAccount(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Account closed") String reason) {
        commandService.closeAccount(id, reason);
    }

    // ==================== QUERIES (Read Side) ====================

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(queryService.getAllAccounts());
    }

    @GetMapping("/accounts/paged")
    public ResponseEntity<Page<AccountResponse>> getAllAccountsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.getAllAccountsPaged(page, size));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getAccountById(id));
    }

    @GetMapping("/accounts/{id}/balance")
    public ResponseEntity<AccountResponse> getAccountBalance(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getAccountBalance(id));
    }

    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getTransactionHistory(id));
    }

    @GetMapping("/accounts/{id}/transactions/paged")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistoryPaged(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.getTransactionHistoryPaged(id, page, size));
    }

    @GetMapping("/accounts/{id}/events")
    public ResponseEntity<List<EventResponse>> getAccountEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getEventHistory(id));
    }

    @GetMapping("/events")
    public ResponseEntity<Page<EventResponse>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.getAllEvents(page, size));
    }
}
