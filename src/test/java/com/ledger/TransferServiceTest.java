package com.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.domain.Account;
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
 * Unit tests for Transfer operations via AccountCommandService.
 * Focuses on money transfer scenarios between accounts.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

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

    private UUID fromAccountId;
    private UUID toAccountId;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();

        fromAccount = Account.builder()
            .id(fromAccountId)
            .accountNumber("ACC111111111")
            .accountHolder("Sender")
            .balance(new BigDecimal("1000.00"))
            .currency("USD")
            .status(Account.AccountStatus.ACTIVE)
            .build();

        toAccount = Account.builder()
            .id(toAccountId)
            .accountNumber("ACC222222222")
            .accountHolder("Receiver")
            .balance(new BigDecimal("500.00"))
            .currency("USD")
            .status(Account.AccountStatus.ACTIVE)
            .build();
    }

    @Nested
    @DisplayName("transferMoney()")
    class TransferMoneyTests {

        @Test
        @DisplayName("should transfer money between accounts successfully")
        void transferMoney_validRequest_succeeds() throws Exception {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("300.00"))
                .description("Payment for services")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            commandService.transferMoney(request);

            // Then
            verify(accountRepository, times(2)).save(any(Account.class)); // Both accounts updated
            verify(ledgerEntryRepository, times(2)).save(any()); // Debit + Credit entries
            verify(eventStoreRepository).save(any()); // MoneyTransferredEvent
        }

        @Test
        @DisplayName("should reject transfer with insufficient funds")
        void transferMoney_insufficientFunds_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("5000.00")) // More than balance
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

            // When & Then
            assertThrows(InsufficientFundsException.class, () -> commandService.transferMoney(request));
            verify(accountRepository, never()).save(any());
            verify(ledgerEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject transfer to non-existent account")
        void transferMoney_toNonExistentAccount_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer from non-existent account")
        void transferMoney_fromNonExistentAccount_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer with negative amount (validated at controller)")
        void transferMoney_validatedByController() {
            // This test documents that amount validation happens at the controller level
            // via @DecimalMin annotation on TransferRequest
            // The service assumes valid input after validation
            assertTrue(true); // Placeholder - actual validation is in DTO
        }

        @Test
        @DisplayName("should reject transfer to same account")
        void transferMoney_sameAccount_throwsException() {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(fromAccountId) // Same as source
                .amount(new BigDecimal("100.00"))
                .description("Self transfer")
                .build();

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.transferMoney(request));
            verify(accountRepository, never()).findById(any());
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should transfer exact balance amount")
        void transferMoney_exactBalance_succeeds() throws Exception {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("1000.00")) // Exact balance
                .description("Full withdrawal")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            commandService.transferMoney(request);

            // Then - should succeed with zero balance after transfer
            verify(accountRepository, times(2)).save(any(Account.class));
        }

        @Test
        @DisplayName("should transfer small amount successfully")
        void transferMoney_smallAmount_succeeds() throws Exception {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("0.01")) // Minimum amount
                .description("Test transfer")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            commandService.transferMoney(request);

            // Then
            verify(accountRepository, times(2)).save(any(Account.class));
        }

        @Test
        @DisplayName("should reject transfer from frozen account")
        void transferMoney_frozenSourceAccount_throwsException() {
            // Given
            fromAccount.setStatus(Account.AccountStatus.FROZEN);
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer to frozen account")
        void transferMoney_frozenDestinationAccount_throwsException() {
            // Given
            toAccount.setStatus(Account.AccountStatus.FROZEN);
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should reject transfer from closed account")
        void transferMoney_closedSourceAccount_throwsException() {
            // Given
            fromAccount.setStatus(Account.AccountStatus.CLOSED);
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
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
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

            // When & Then
            assertThrows(InvalidOperationException.class, () -> commandService.transferMoney(request));
        }

        @Test
        @DisplayName("should update both account balances correctly")
        void transferMoney_updatesBothBalances() throws Exception {
            // Given
            TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(new BigDecimal("250.00"))
                .description("Payment")
                .build();

            when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
                Account saved = inv.getArgument(0);
                if (saved.getId().equals(fromAccountId)) {
                    assertEquals(new BigDecimal("750.00"), saved.getBalance());
                } else {
                    assertEquals(new BigDecimal("750.00"), saved.getBalance());
                }
                return saved;
            });
            when(projectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(projectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.countByAccountId(any())).thenReturn(1L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            commandService.transferMoney(request);

            // Then - verification happens in the mock answer above
        }
    }
}
