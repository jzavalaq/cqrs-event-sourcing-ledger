package com.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.domain.Account;
import com.ledger.domain.LedgerEntry;
import com.ledger.dto.CreditDebitRequest;
import com.ledger.dto.OpenAccountRequest;
import com.ledger.dto.TransferRequest;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.exception.InvalidOperationException;
import com.ledger.exception.ResourceNotFoundException;
import com.ledger.repository.AccountProjectionRepository;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.EventStoreRepository;
import com.ledger.repository.LedgerEntryRepository;
import com.ledger.service.AccountCommandService;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountCommandService (CQRS Write Side).
 * Uses Mockito to isolate service logic from persistence layer.
 */
@ExtendWith(MockitoExtension.class)
class AccountCommandServiceTest {

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
    private Account testAccount;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        testAccount = Account.builder()
            .id(accountId)
            .accountNumber("ACC123456789")
            .accountHolder("John Doe")
            .balance(new BigDecimal("1000.00"))
            .currency("USD")
            .status(Account.AccountStatus.ACTIVE)
            .build();
    }

    @Nested
    @DisplayName("openAccount()")
    class OpenAccountTests {

        @Test
        @DisplayName("should open new account with valid request")
        void openAccount_validRequest_returnsAccount() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber("ACC123456789")).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            Account result = commandService.openAccount(request);

            // Then
            assertNotNull(result);
            assertEquals("ACC123456789", result.getAccountNumber());
            assertEquals("John Doe", result.getAccountHolder());
            assertEquals(new BigDecimal("500.00"), result.getBalance());
            assertEquals("USD", result.getCurrency());
            assertEquals(Account.AccountStatus.ACTIVE, result.getStatus());

            verify(accountRepository).save(any(Account.class));
            verify(eventStoreRepository, times(2)).save(any()); // AccountOpenedEvent + AccountCreditedEvent
        }

        @Test
        @DisplayName("should open account with zero initial balance")
        void openAccount_zeroInitialBalance_succeeds() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC000000001")
                .accountHolder("Jane Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber("ACC000000001")).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(accountId);
                return a;
            });
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            Account result = commandService.openAccount(request);

            // Then
            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result.getBalance());
            // Only AccountOpenedEvent, no AccountCreditedEvent for zero balance
            verify(eventStoreRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("should reject duplicate account number")
        void openAccount_duplicateAccountNumber_throwsException() {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency("USD")
                .build();

            when(accountRepository.existsByAccountNumber("ACC123456789")).thenReturn(true);

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.openAccount(request));
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("creditAccount()")
    class CreditAccountTests {

        @Test
        @DisplayName("should credit account with valid amount")
        void creditAccount_validAmount_updatesBalance() throws Exception {
            // Given
            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("250.00"))
                .description("Deposit")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            Account result = commandService.creditAccount(accountId, request);

            // Then
            assertEquals(new BigDecimal("1250.00"), result.getBalance());
            verify(ledgerEntryRepository).save(any(LedgerEntry.class));
            verify(eventStoreRepository).save(any());
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void creditAccount_accountNotFound_throwsException() {
            // Given
            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("100.00"))
                .description("Deposit")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> commandService.creditAccount(accountId, request));
        }

        @Test
        @DisplayName("should throw exception when account is closed")
        void creditAccount_closedAccount_throwsException() {
            // Given
            testAccount.setStatus(Account.AccountStatus.CLOSED);
            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("100.00"))
                .description("Deposit")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.creditAccount(accountId, request));
        }
    }

    @Nested
    @DisplayName("debitAccount()")
    class DebitAccountTests {

        @Test
        @DisplayName("should debit account with valid amount")
        void debitAccount_validAmount_updatesBalance() throws Exception {
            // Given
            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("300.00"))
                .description("Withdrawal")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            Account result = commandService.debitAccount(accountId, request);

            // Then
            assertEquals(new BigDecimal("700.00"), result.getBalance());
            verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("should reject debit with insufficient funds")
        void debitAccount_insufficientFunds_throwsException() {
            // Given
            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("2000.00")) // More than balance
                .description("Withdrawal")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

            // When & Then
            assertThrows(InsufficientFundsException.class, () -> commandService.debitAccount(accountId, request));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject debit from closed account")
        void debitAccount_closedAccount_throwsException() {
            // Given
            testAccount.setStatus(Account.AccountStatus.CLOSED);
            CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("100.00"))
                .description("Withdrawal")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.debitAccount(accountId, request));
        }
    }

    @Nested
    @DisplayName("closeAccount()")
    class CloseAccountTests {

        @Test
        @DisplayName("should close account with zero balance")
        void closeAccount_zeroBalance_succeeds() throws Exception {
            // Given
            testAccount.setBalance(BigDecimal.ZERO);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(accountId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            commandService.closeAccount(accountId, "Account closed by user");

            // Then
            verify(accountRepository).save(any(Account.class));
            verify(eventStoreRepository).save(any());
        }

        @Test
        @DisplayName("should reject close with non-zero balance")
        void closeAccount_nonZeroBalance_throwsException() {
            // Given
            testAccount.setBalance(new BigDecimal("100.00"));
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.closeAccount(accountId, "Test"));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when account not found for close")
        void closeAccount_accountNotFound_throwsException() {
            // Given
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> commandService.closeAccount(accountId, "Test"));
        }
    }

    @Nested
    @DisplayName("transferMoney()")
    class TransferMoneyTests {

        private UUID toAccountId;
        private Account toAccount;

        @BeforeEach
        void setUpTransfer() {
            toAccountId = UUID.randomUUID();
            toAccount = Account.builder()
                .id(toAccountId)
                .accountNumber("ACC987654321")
                .accountHolder("Jane Doe")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();
        }

        @Test
        @DisplayName("should transfer money between accounts")
        void transferMoney_validRequest_succeeds() throws Exception {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("300.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            commandService.transferMoney(request);

            // Then
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository, times(2)).save(accountCaptor.capture());

            verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class)); // Debit + Credit entries
            verify(eventStoreRepository).save(any()); // MoneyTransferredEvent
        }

        @Test
        @DisplayName("should reject transfer to same account")
        void transferMoney_sameAccount_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(accountId)
                .amount(new BigDecimal("100.00"))
                .description("Self transfer")
                .build();

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.transferMoney(request));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject transfer with insufficient funds")
        void transferMoney_insufficientFunds_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("5000.00")) // More than balance
                .description("Payment")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

            // When & Then
            assertThrows(InsufficientFundsException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer when source account not found")
        void transferMoney_sourceNotFound_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer when destination account not found")
        void transferMoney_destinationNotFound_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer from closed account")
        void transferMoney_closedSourceAccount_throwsException() {
            // Given
            testAccount.setStatus(Account.AccountStatus.CLOSED);
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer to closed account")
        void transferMoney_closedDestinationAccount_throwsException() {
            // Given
            toAccount.setStatus(Account.AccountStatus.CLOSED);
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.transferMoney(request));
        }
    }
}
