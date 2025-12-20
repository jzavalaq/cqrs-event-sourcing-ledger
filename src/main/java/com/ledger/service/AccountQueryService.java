package com.ledger.service;

import com.ledger.domain.Account;
import com.ledger.domain.AccountProjection;
import com.ledger.domain.LedgerEntry;
import com.ledger.dto.AccountResponse;
import com.ledger.dto.EventResponse;
import com.ledger.dto.TransactionResponse;
import com.ledger.event.StoredEvent;
import com.ledger.exception.ResourceNotFoundException;
import com.ledger.repository.AccountProjectionRepository;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.EventStoreRepository;
import com.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query service for account operations (CQRS Read Side).
 * Uses projections for optimized reads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final AccountProjectionRepository projectionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final EventStoreRepository eventStoreRepository;

    public AccountResponse getAccountById(UUID accountId) {
        AccountProjection projection = projectionRepository.findById(accountId)
            .orElseThrow(() -> ResourceNotFoundException.accountNotFound(accountId));
        return toAccountResponse(projection);
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        AccountProjection projection = projectionRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
        return toAccountResponse(projection);
    }

    public AccountResponse getAccountBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> ResourceNotFoundException.accountNotFound(accountId));
        return AccountResponse.builder()
            .id(account.getId())
            .accountNumber(account.getAccountNumber())
            .balance(account.getBalance())
            .currency(account.getCurrency())
            .status(account.getStatus().name())
            .build();
    }

    public List<AccountResponse> getAllAccounts() {
        return projectionRepository.findAll().stream()
            .map(this::toAccountResponse)
            .collect(Collectors.toList());
    }

    public Page<AccountResponse> getAllAccountsPaged(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return projectionRepository.findAll(pageable)
            .map(this::toAccountResponse);
    }

    public List<TransactionResponse> getTransactionHistory(UUID accountId) {
        if (!projectionRepository.existsById(accountId)) {
            throw ResourceNotFoundException.accountNotFound(accountId);
        }
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
            .map(this::toTransactionResponse)
            .collect(Collectors.toList());
    }

    public Page<TransactionResponse> getTransactionHistoryPaged(UUID accountId, int page, int size) {
        if (!projectionRepository.existsById(accountId)) {
            throw ResourceNotFoundException.accountNotFound(accountId);
        }
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
            .map(this::toTransactionResponse);
    }

    public List<EventResponse> getEventHistory(UUID accountId) {
        return eventStoreRepository.findByAggregateIdOrderByOccurredAtAsc(accountId).stream()
            .map(this::toEventResponse)
            .collect(Collectors.toList());
    }

    public Page<EventResponse> getAllEvents(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return eventStoreRepository.findAllByOrderByOccurredAtDesc(pageable)
            .map(this::toEventResponse);
    }

    private AccountResponse toAccountResponse(AccountProjection projection) {
        return AccountResponse.builder()
            .id(projection.getId())
            .accountNumber(projection.getAccountNumber())
            .accountHolder(projection.getAccountHolder())
            .balance(projection.getBalance())
            .currency(projection.getCurrency())
            .status(projection.getStatus().name())
            .transactionCount(projection.getTransactionCount())
            .createdAt(projection.getCreatedAt())
            .updatedAt(projection.getUpdatedAt())
            .version(projection.getVersion())
            .build();
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
            .id(account.getId())
            .accountNumber(account.getAccountNumber())
            .accountHolder(account.getAccountHolder())
            .balance(account.getBalance())
            .currency(account.getCurrency())
            .status(account.getStatus().name())
            .createdAt(account.getCreatedAt())
            .updatedAt(account.getUpdatedAt())
            .version(account.getVersion())
            .build();
    }

    private TransactionResponse toTransactionResponse(LedgerEntry entry) {
        return TransactionResponse.builder()
            .id(entry.getEntryId())
            .accountId(entry.getAccountId())
            .type(entry.getEntryType().name())
            .amount(entry.getAmount())
            .balanceAfter(entry.getBalanceAfter())
            .description(entry.getDescription())
            .relatedAccountId(entry.getRelatedAccountId())
            .createdAt(entry.getCreatedAt())
            .build();
    }

    private EventResponse toEventResponse(StoredEvent event) {
        return EventResponse.builder()
            .id(event.getId())
            .eventId(event.getEventId())
            .aggregateId(event.getAggregateId())
            .aggregateType(event.getAggregateType())
            .eventType(event.getEventType())
            .eventData(event.getEventData())
            .occurredAt(event.getOccurredAt())
            .build();
    }
}
