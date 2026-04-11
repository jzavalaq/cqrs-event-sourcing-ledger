package com.ledger;

import com.ledger.domain.Account;
import com.ledger.dto.CreditDebitRequest;
import com.ledger.dto.OpenAccountRequest;
import com.ledger.dto.TransferRequest;
import com.ledger.event.StoredEvent;
import com.ledger.repository.AccountProjectionRepository;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.EventStoreRepository;
import com.ledger.repository.LedgerEntryRepository;
import com.ledger.service.AccountCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Event Store operations via AccountCommandService.
 * Verifies that domain events are properly stored during operations.
 */
@ExtendWith(MockitoExtension.class)
class EventStoreTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountProjectionRepository projectionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private EventStoreRepository eventStoreRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AccountCommandService commandService;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Event Appending")
    class EventAppendingTests {

        @Test
        @DisplayName("should append AccountOpenedEvent when account is opened")
        void appendEvent_accountOpened_savesEvent() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"AccountOpenedEvent\"}");

            // When
            commandService.openAccount(request);

            // Then
            verify(eventStoreRepository).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("should append AccountCreditedEvent when account is credited")
        void appendEvent_accountCredited_savesEvent() throws Exception {
            // Given
            Account account = Account.builder()
                .id(accountId)
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();

            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("100.00"))
                .description("Deposit")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"AccountCreditedEvent\"}");

            // When
            commandService.creditAccount(accountId, request);

            // Then
            verify(eventStoreRepository).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("should append AccountDebitedEvent when account is debited")
        void appendEvent_accountDebited_savesEvent() throws Exception {
            // Given
            Account account = Account.builder()
                .id(accountId)
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();

            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("100.00"))
                .description("Withdrawal")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"AccountDebitedEvent\"}");

            // When
            commandService.debitAccount(accountId, request);

            // Then
            verify(eventStoreRepository).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("should append MoneyTransferredEvent when transfer occurs")
        void appendEvent_moneyTransferred_savesEvent() throws Exception {
            // Given
            UUID toAccountId = UUID.randomUUID();
            Account fromAccount = Account.builder()
                .id(accountId)
                .accountNumber("ACC111111111")
                .accountHolder("Sender")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();

            Account toAccount = Account.builder()
                .id(toAccountId)
                .accountNumber("ACC222222222")
                .accountHolder("Receiver")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();

            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("200.00"))
                .description("Transfer")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"MoneyTransferredEvent\"}");

            // When
            commandService.transferMoney(request);

            // Then
            verify(eventStoreRepository).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("should append AccountClosedEvent when account is closed")
        void appendEvent_accountClosed_savesEvent() throws Exception {
            // Given
            Account account = Account.builder()
                .id(accountId)
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(accountId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"AccountClosedEvent\"}");

            // When
            commandService.closeAccount(accountId, "Customer request");

            // Then
            verify(eventStoreRepository).save(any(StoredEvent.class));
        }
    }

    @Nested
    @DisplayName("Event Data Integrity")
    class EventDataIntegrityTests {

        @Test
        @DisplayName("should store event with correct aggregate ID")
        void eventData_correctAggregateId() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
            when(accountRepository.save(accountCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            ArgumentCaptor<StoredEvent> eventCaptor = ArgumentCaptor.forClass(StoredEvent.class);

            // When
            commandService.openAccount(request);

            // Then
            verify(eventStoreRepository).save(eventCaptor.capture());
            // The aggregate ID in the event should match the account ID that was set in the service
            assertNotNull(eventCaptor.getValue().getAggregateId());
            assertEquals(accountCaptor.getValue().getId(), eventCaptor.getValue().getAggregateId());
        }

        @Test
        @DisplayName("should store event with correct aggregate type")
        void eventData_correctAggregateType() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            ArgumentCaptor<StoredEvent> eventCaptor = ArgumentCaptor.forClass(StoredEvent.class);

            // When
            commandService.openAccount(request);

            // Then
            verify(eventStoreRepository).save(eventCaptor.capture());
            assertEquals("Account", eventCaptor.getValue().getAggregateType());
        }

        @Test
        @DisplayName("should store event with occurredAt timestamp")
        void eventData_hasTimestamp() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            ArgumentCaptor<StoredEvent> eventCaptor = ArgumentCaptor.forClass(StoredEvent.class);

            // When
            commandService.openAccount(request);

            // Then
            verify(eventStoreRepository).save(eventCaptor.capture());
            assertNotNull(eventCaptor.getValue().getOccurredAt());
            assertTrue(eventCaptor.getValue().getOccurredAt().isBefore(Instant.now().plusSeconds(1)));
        }

        @Test
        @DisplayName("should store event with unique event ID")
        void eventData_uniqueEventId() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            ArgumentCaptor<StoredEvent> eventCaptor = ArgumentCaptor.forClass(StoredEvent.class);

            // When
            commandService.openAccount(request);

            // Then
            verify(eventStoreRepository).save(eventCaptor.capture());
            assertNotNull(eventCaptor.getValue().getEventId());
        }

        @Test
        @DisplayName("should store event with correct event type")
        void eventData_correctEventType() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            ArgumentCaptor<StoredEvent> eventCaptor = ArgumentCaptor.forClass(StoredEvent.class);

            // When
            commandService.openAccount(request);

            // Then
            verify(eventStoreRepository).save(eventCaptor.capture());
            assertEquals("AccountOpenedEvent", eventCaptor.getValue().getEventType());
        }
    }

    @Nested
    @DisplayName("Event Order")
    class EventOrderTests {

        @Test
        @DisplayName("should append multiple events for single operation")
        void eventOrder_multipleEventsForOpenWithInitialBalance() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(new BigDecimal("500.00")) // Non-zero initial balance
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any())).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            commandService.openAccount(request);

            // Then - Two events: AccountOpenedEvent and AccountCreditedEvent
            verify(eventStoreRepository, times(2)).save(any(StoredEvent.class));
        }
    }
}
