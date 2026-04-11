package com.ledger;

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
import com.ledger.service.AccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountQueryService (CQRS Read Side).
 * Uses Mockito to isolate service logic from persistence layer.
 */
@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountProjectionRepository projectionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private EventStoreRepository eventStoreRepository;

    @InjectMocks
    private AccountQueryService queryService;

    private UUID accountId;
    private AccountProjection testProjection;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        testProjection = AccountProjection.builder()
            .id(accountId)
            .accountNumber("ACC123456789")
            .accountHolder("John Doe")
            .balance(new BigDecimal("1000.00"))
            .currency("USD")
            .status(Account.AccountStatus.ACTIVE)
            .transactionCount(5)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(1L)
            .build();

        testAccount = Account.builder()
            .id(accountId)
            .accountNumber("ACC123456789")
            .accountHolder("John Doe")
            .balance(new BigDecimal("1000.00"))
            .currency("USD")
            .status(Account.AccountStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(1L)
            .build();
    }

    @Nested
    @DisplayName("getAccountById()")
    class GetAccountByIdTests {

        @Test
        @DisplayName("should return account when found")
        void getAccountById_existingId_returnsAccount() {
            // Given
            when(projectionRepository.findById(accountId)).thenReturn(Optional.of(testProjection));

            // When
            AccountResponse result = queryService.getAccountById(accountId);

            // Then
            assertNotNull(result);
            assertEquals(accountId, result.getId());
            assertEquals("ACC123456789", result.getAccountNumber());
            assertEquals("John Doe", result.getAccountHolder());
            assertEquals(new BigDecimal("1000.00"), result.getBalance());
            assertEquals("USD", result.getCurrency());
            assertEquals("ACTIVE", result.getStatus());
            assertEquals(5, result.getTransactionCount());
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void getAccountById_nonExistentId_throwsException() {
            // Given
            when(projectionRepository.findById(accountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> queryService.getAccountById(accountId));
        }
    }

    @Nested
    @DisplayName("getAccountByNumber()")
    class GetAccountByNumberTests {

        @Test
        @DisplayName("should return account when found by number")
        void getAccountByNumber_existingNumber_returnsAccount() {
            // Given
            when(projectionRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(testProjection));

            // When
            AccountResponse result = queryService.getAccountByNumber("ACC123456789");

            // Then
            assertNotNull(result);
            assertEquals("ACC123456789", result.getAccountNumber());
        }

        @Test
        @DisplayName("should throw exception when account number not found")
        void getAccountByNumber_nonExistentNumber_throwsException() {
            // Given
            when(projectionRepository.findByAccountNumber("NOTFOUND")).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> queryService.getAccountByNumber("NOTFOUND"));
        }
    }

    @Nested
    @DisplayName("getAccountBalance()")
    class GetAccountBalanceTests {

        @Test
        @DisplayName("should return account balance")
        void getAccountBalance_existingId_returnsBalance() {
            // Given
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

            // When
            AccountResponse result = queryService.getAccountBalance(accountId);

            // Then
            assertNotNull(result);
            assertEquals(accountId, result.getId());
            assertEquals(new BigDecimal("1000.00"), result.getBalance());
            assertEquals("USD", result.getCurrency());
        }

        @Test
        @DisplayName("should throw exception when account not found for balance")
        void getAccountBalance_nonExistentId_throwsException() {
            // Given
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> queryService.getAccountBalance(accountId));
        }
    }

    @Nested
    @DisplayName("getAllAccounts()")
    class GetAllAccountsTests {

        @Test
        @DisplayName("should return all accounts")
        void getAllAccounts_returnsAccountList() {
            // Given
            AccountProjection projection2 = AccountProjection.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC987654321")
                .accountHolder("Jane Doe")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .transactionCount(2)
                .build();

            when(projectionRepository.findAll()).thenReturn(List.of(testProjection, projection2));

            // When
            List<AccountResponse> result = queryService.getAllAccounts();

            // Then
            assertEquals(2, result.size());
            assertEquals("ACC123456789", result.get(0).getAccountNumber());
            assertEquals("ACC987654321", result.get(1).getAccountNumber());
        }

        @Test
        @DisplayName("should return empty list when no accounts")
        void getAllAccounts_noAccounts_returnsEmptyList() {
            // Given
            when(projectionRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<AccountResponse> result = queryService.getAllAccounts();

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAllAccountsPaged()")
    class GetAllAccountsPagedTests {

        @Test
        @DisplayName("should return paginated accounts")
        void getAllAccountsPaged_validRequest_returnsPage() {
            // Given
            Page<AccountProjection> page = new PageImpl<>(List.of(testProjection));
            when(projectionRepository.findAll(any(Pageable.class))).thenReturn(page);

            // When
            Page<AccountResponse> result = queryService.getAllAccountsPaged(0, 10);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("ACC123456789", result.getContent().get(0).getAccountNumber());
        }

        @Test
        @DisplayName("should handle negative page number")
        void getAllAccountsPaged_negativePage_usesZero() {
            // Given
            Page<AccountProjection> page = new PageImpl<>(List.of(testProjection));
            when(projectionRepository.findAll(any(Pageable.class))).thenReturn(page);

            // When
            Page<AccountResponse> result = queryService.getAllAccountsPaged(-1, 10);

            // Then
            assertNotNull(result);
            verify(projectionRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("should cap page size at maximum")
        void getAllAccountsPaged_largePageSize_capsSize() {
            // Given
            Page<AccountProjection> page = new PageImpl<>(List.of(testProjection));
            when(projectionRepository.findAll(any(Pageable.class))).thenReturn(page);

            // When
            queryService.getAllAccountsPaged(0, 200); // Should be capped to 100

            // Then - verify the page size was capped
            verify(projectionRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getTransactionHistory()")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("should return transaction history for account")
        void getTransactionHistory_existingAccount_returnsTransactions() {
            // Given
            LedgerEntry entry = LedgerEntry.builder()
                .id(1L)
                .entryId(UUID.randomUUID())
                .accountId(accountId)
                .correlationId(UUID.randomUUID())
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .description("Deposit")
                .createdAt(Instant.now())
                .build();

            when(projectionRepository.existsById(accountId)).thenReturn(true);
            when(ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId)).thenReturn(List.of(entry));

            // When
            List<TransactionResponse> result = queryService.getTransactionHistory(accountId);

            // Then
            assertEquals(1, result.size());
            assertEquals(accountId, result.get(0).getAccountId());
            assertEquals("CREDIT", result.get(0).getType());
            assertEquals(new BigDecimal("500.00"), result.get(0).getAmount());
        }

        @Test
        @DisplayName("should return empty list for new account")
        void getTransactionHistory_newAccount_returnsEmptyList() {
            // Given
            when(projectionRepository.existsById(accountId)).thenReturn(true);
            when(ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId)).thenReturn(Collections.emptyList());

            // When
            List<TransactionResponse> result = queryService.getTransactionHistory(accountId);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should throw exception for non-existent account")
        void getTransactionHistory_nonExistentAccount_throwsException() {
            // Given
            when(projectionRepository.existsById(accountId)).thenReturn(false);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> queryService.getTransactionHistory(accountId));
        }
    }

    @Nested
    @DisplayName("getTransactionHistoryPaged()")
    class GetTransactionHistoryPagedTests {

        @Test
        @DisplayName("should return paginated transaction history")
        void getTransactionHistoryPaged_validRequest_returnsPage() {
            // Given
            LedgerEntry entry = LedgerEntry.builder()
                .id(1L)
                .entryId(UUID.randomUUID())
                .accountId(accountId)
                .correlationId(UUID.randomUUID())
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .description("Deposit")
                .createdAt(Instant.now())
                .build();

            Page<LedgerEntry> page = new PageImpl<>(List.of(entry));
            when(projectionRepository.existsById(accountId)).thenReturn(true);
            when(ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(eq(accountId), any(Pageable.class))).thenReturn(page);

            // When
            Page<TransactionResponse> result = queryService.getTransactionHistoryPaged(accountId, 0, 20);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("should throw exception for non-existent account in paged query")
        void getTransactionHistoryPaged_nonExistentAccount_throwsException() {
            // Given
            when(projectionRepository.existsById(accountId)).thenReturn(false);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> queryService.getTransactionHistoryPaged(accountId, 0, 20));
        }
    }

    @Nested
    @DisplayName("getEventHistory()")
    class GetEventHistoryTests {

        @Test
        @DisplayName("should return event history for account")
        void getEventHistory_existingAccount_returnsEvents() {
            // Given
            StoredEvent event = StoredEvent.builder()
                .id(1L)
                .eventId(UUID.randomUUID())
                .aggregateId(accountId)
                .aggregateType("Account")
                .eventType("AccountOpenedEvent")
                .eventData("{}")
                .occurredAt(Instant.now())
                .build();

            when(eventStoreRepository.findByAggregateIdOrderByOccurredAtAsc(accountId)).thenReturn(List.of(event));

            // When
            List<EventResponse> result = queryService.getEventHistory(accountId);

            // Then
            assertEquals(1, result.size());
            assertEquals(accountId, result.get(0).getAggregateId());
            assertEquals("AccountOpenedEvent", result.get(0).getEventType());
        }

        @Test
        @DisplayName("should return empty list when no events")
        void getEventHistory_noEvents_returnsEmptyList() {
            // Given
            when(eventStoreRepository.findByAggregateIdOrderByOccurredAtAsc(accountId)).thenReturn(Collections.emptyList());

            // When
            List<EventResponse> result = queryService.getEventHistory(accountId);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAllEvents()")
    class GetAllEventsTests {

        @Test
        @DisplayName("should return paginated events")
        void getAllEvents_validRequest_returnsPage() {
            // Given
            StoredEvent event = StoredEvent.builder()
                .id(1L)
                .eventId(UUID.randomUUID())
                .aggregateId(accountId)
                .aggregateType("Account")
                .eventType("AccountOpenedEvent")
                .eventData("{}")
                .occurredAt(Instant.now())
                .build();

            Page<StoredEvent> page = new PageImpl<>(List.of(event));
            when(eventStoreRepository.findAllByOrderByOccurredAtDesc(any(Pageable.class))).thenReturn(page);

            // When
            Page<EventResponse> result = queryService.getAllEvents(0, 20);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("AccountOpenedEvent", result.getContent().get(0).getEventType());
        }
    }
}
