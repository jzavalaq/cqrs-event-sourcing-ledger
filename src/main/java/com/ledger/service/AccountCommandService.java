package com.ledger.service;

import com.ledger.domain.Account;
import com.ledger.domain.AccountProjection;
import com.ledger.domain.LedgerEntry;
import com.ledger.dto.CreditDebitRequest;
import com.ledger.dto.OpenAccountRequest;
import com.ledger.dto.TransferRequest;
import com.ledger.event.*;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.exception.InvalidOperationException;
import com.ledger.exception.ResourceNotFoundException;
import com.ledger.repository.AccountProjectionRepository;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.EventStoreRepository;
import com.ledger.repository.LedgerEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Command service for account operations (CQRS Write Side).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountCommandService {

    private final AccountRepository accountRepository;
    private final AccountProjectionRepository projectionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Account openAccount(OpenAccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new InvalidOperationException("Account number already exists: " + request.getAccountNumber());
        }

        UUID accountId = UUID.randomUUID();
        Instant now = Instant.now();

        Account account = Account.builder()
            .id(accountId)
            .accountNumber(request.getAccountNumber())
            .accountHolder(request.getAccountHolder())
            .balance(request.getInitialBalance())
            .currency(request.getCurrency())
            .status(Account.AccountStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

        Account saved = accountRepository.save(account);

        AccountOpenedEvent event = new AccountOpenedEvent(
            accountId, request.getAccountNumber(), request.getAccountHolder(),
            request.getInitialBalance(), request.getCurrency()
        );
        appendEvent(event, accountId, "Account");
        updateProjection(saved);

        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            UUID transactionId = UUID.randomUUID();
            LedgerEntry entry = LedgerEntry.builder()
                .entryId(UUID.randomUUID())
                .accountId(accountId)
                .correlationId(transactionId)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(request.getInitialBalance())
                .balanceAfter(request.getInitialBalance())
                .description("Initial deposit")
                .createdAt(now)
                .build();
            ledgerEntryRepository.save(entry);

            AccountCreditedEvent creditEvent = new AccountCreditedEvent(
                accountId, request.getInitialBalance(), request.getInitialBalance(),
                "Initial deposit", transactionId
            );
            appendEvent(creditEvent, accountId, "Account");
        }

        log.info("Account opened: {}", accountId);
        return saved;
    }

    @Transactional
    public Account creditAccount(UUID accountId, CreditDebitRequest request) {
        Account account = findAccountById(accountId);
        validateAccountIsActive(account);

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        account.setUpdatedAt(Instant.now());

        Account saved = accountRepository.save(account);

        UUID transactionId = UUID.randomUUID();
        LedgerEntry entry = LedgerEntry.builder()
            .entryId(UUID.randomUUID())
            .accountId(accountId)
            .correlationId(transactionId)
            .entryType(LedgerEntry.EntryType.CREDIT)
            .amount(request.getAmount())
            .balanceAfter(newBalance)
            .description(request.getDescription())
            .createdAt(Instant.now())
            .build();
        ledgerEntryRepository.save(entry);

        AccountCreditedEvent event = new AccountCreditedEvent(
            accountId, request.getAmount(), newBalance, request.getDescription(), transactionId
        );
        appendEvent(event, accountId, "Account");
        updateProjection(saved);

        log.info("Account credited: {} +{}", accountId, request.getAmount());
        return saved;
    }

    @Transactional
    public Account debitAccount(UUID accountId, CreditDebitRequest request) {
        Account account = findAccountById(accountId);
        validateAccountIsActive(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw InsufficientFundsException.forAccount(accountId, request.getAmount(), account.getBalance());
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        account.setUpdatedAt(Instant.now());

        Account saved = accountRepository.save(account);

        UUID transactionId = UUID.randomUUID();
        LedgerEntry entry = LedgerEntry.builder()
            .entryId(UUID.randomUUID())
            .accountId(accountId)
            .correlationId(transactionId)
            .entryType(LedgerEntry.EntryType.DEBIT)
            .amount(request.getAmount())
            .balanceAfter(newBalance)
            .description(request.getDescription())
            .createdAt(Instant.now())
            .build();
        ledgerEntryRepository.save(entry);

        AccountDebitedEvent event = new AccountDebitedEvent(
            accountId, request.getAmount(), newBalance, request.getDescription(), transactionId
        );
        appendEvent(event, accountId, "Account");
        updateProjection(saved);

        log.info("Account debited: {} -{}", accountId, request.getAmount());
        return saved;
    }

    @Transactional
    public void transferMoney(TransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new InvalidOperationException("Cannot transfer to the same account");
        }

        Account fromAccount = findAccountById(request.getFromAccountId());
        Account toAccount = findAccountById(request.getToAccountId());

        validateAccountIsActive(fromAccount);
        validateAccountIsActive(toAccount);

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw InsufficientFundsException.forAccount(
                request.getFromAccountId(), request.getAmount(), fromAccount.getBalance()
            );
        }

        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now();

        BigDecimal fromNewBalance = fromAccount.getBalance().subtract(request.getAmount());
        fromAccount.setBalance(fromNewBalance);
        fromAccount.setUpdatedAt(now);

        BigDecimal toNewBalance = toAccount.getBalance().add(request.getAmount());
        toAccount.setBalance(toNewBalance);
        toAccount.setUpdatedAt(now);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        LedgerEntry debitEntry = LedgerEntry.builder()
            .entryId(UUID.randomUUID())
            .accountId(request.getFromAccountId())
            .correlationId(transactionId)
            .entryType(LedgerEntry.EntryType.DEBIT)
            .amount(request.getAmount())
            .balanceAfter(fromNewBalance)
            .description("Transfer to " + toAccount.getAccountNumber() + ": " + request.getDescription())
            .relatedAccountId(request.getToAccountId())
            .createdAt(now)
            .build();
        ledgerEntryRepository.save(debitEntry);

        LedgerEntry creditEntry = LedgerEntry.builder()
            .entryId(UUID.randomUUID())
            .accountId(request.getToAccountId())
            .correlationId(transactionId)
            .entryType(LedgerEntry.EntryType.CREDIT)
            .amount(request.getAmount())
            .balanceAfter(toNewBalance)
            .description("Transfer from " + fromAccount.getAccountNumber() + ": " + request.getDescription())
            .relatedAccountId(request.getFromAccountId())
            .createdAt(now)
            .build();
        ledgerEntryRepository.save(creditEntry);

        MoneyTransferredEvent event = new MoneyTransferredEvent(
            request.getFromAccountId(), request.getToAccountId(),
            request.getAmount(), request.getDescription(), transactionId
        );
        appendEvent(event, request.getFromAccountId(), "Account");

        updateProjection(fromAccount);
        updateProjection(toAccount);

        log.info("Transfer completed: {} -> {} amount={}",
            request.getFromAccountId(), request.getToAccountId(), request.getAmount());
    }

    @Transactional
    public void closeAccount(UUID accountId, String reason) {
        Account account = findAccountById(accountId);

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidOperationException("Cannot close account with non-zero balance");
        }

        account.setStatus(Account.AccountStatus.CLOSED);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        AccountClosedEvent event = new AccountClosedEvent(accountId, reason);
        appendEvent(event, accountId, "Account");

        updateProjectionStatus(accountId, Account.AccountStatus.CLOSED);

        log.info("Account closed: {}", accountId);
    }

    private Account findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> ResourceNotFoundException.accountNotFound(accountId));
    }

    private void validateAccountIsActive(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Account is not active: " + account.getStatus());
        }
    }

    private void appendEvent(DomainEvent event, UUID aggregateId, String aggregateType) {
        try {
            StoredEvent storedEvent = StoredEvent.builder()
                .eventId(event.getEventId())
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(event.getEventType())
                .eventData(objectMapper.writeValueAsString(event))
                .occurredAt(event.getOccurredAt())
                .build();
            eventStoreRepository.save(storedEvent);
        } catch (Exception e) {
            log.error("Failed to append event: {}", event.getEventType(), e);
            throw new InvalidOperationException("Failed to store event");
        }
    }

    private void updateProjection(Account account) {
        AccountProjection projection = projectionRepository.findById(account.getId())
            .orElse(AccountProjection.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .createdAt(account.getCreatedAt())
                .transactionCount(0)
                .build());

        projection.setAccountHolder(account.getAccountHolder());
        projection.setBalance(account.getBalance());
        projection.setCurrency(account.getCurrency());
        projection.setStatus(account.getStatus());
        projection.setUpdatedAt(Instant.now());
        projection.setTransactionCount(
            (int) ledgerEntryRepository.countByAccountId(account.getId())
        );

        projectionRepository.save(projection);
    }

    private void updateProjectionStatus(UUID accountId, Account.AccountStatus status) {
        projectionRepository.findById(accountId).ifPresent(projection -> {
            projection.setStatus(status);
            projection.setUpdatedAt(Instant.now());
            projectionRepository.save(projection);
        });
    }
}
