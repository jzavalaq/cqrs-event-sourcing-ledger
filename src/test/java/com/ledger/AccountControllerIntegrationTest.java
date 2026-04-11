package com.ledger;

import com.ledger.config.TestSecurityConfig;
import com.ledger.domain.Account;
import com.ledger.dto.AccountResponse;
import com.ledger.dto.OpenAccountRequest;
import com.ledger.exception.ResourceNotFoundException;
import com.ledger.service.AccountCommandService;
import com.ledger.service.AccountQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Account REST Controller.
 * Uses SpringBootTest with AutoConfigureMockMvc for full context testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountCommandService commandService;

    @MockBean
    private AccountQueryService queryService;

    @Nested
    @DisplayName("POST /api/v1/accounts")
    class CreateAccountTests {

        @Test
        @DisplayName("should create account with valid request - returns 201")
        void createAccount_validRequest_returns201() throws Exception {
            // Given
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .initialBalance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

            Account account = new Account();
            account.setId(UUID.randomUUID());
            account.setAccountNumber("ACC123456789");
            account.setAccountHolder("John Doe");
            account.setBalance(new BigDecimal("1000.00"));
            account.setCurrency("USD");
            account.setStatus(Account.AccountStatus.ACTIVE);

            AccountResponse response = AccountResponse.builder()
                .id(account.getId())
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status("ACTIVE")
                .build();

            when(commandService.openAccount(any())).thenReturn(account);
            when(queryService.getAccountById(any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC123456789"));
        }

        @Test
        @DisplayName("should return 400 for missing account number")
        void createAccount_missingAccountNumber_returns400() throws Exception {
            // Given
            String requestBody = """
                {
                    "accountHolder": "John Doe",
                    "initialBalance": 1000.00,
                    "currency": "USD"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for negative balance")
        void createAccount_negativeBalance_returns400() throws Exception {
            // Given
            String requestBody = """
                {
                    "accountNumber": "ACC123456789",
                    "accountHolder": "John Doe",
                    "initialBalance": -100.00,
                    "currency": "USD"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/{id}")
    class GetAccountTests {

        @Test
        @DisplayName("should return account when found - returns 200")
        void getAccount_existingId_returns200() throws Exception {
            // Given
            UUID accountId = UUID.randomUUID();
            AccountResponse response = AccountResponse.builder()
                .id(accountId)
                .accountNumber("ACC123456789")
                .accountHolder("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status("ACTIVE")
                .build();

            when(queryService.getAccountById(accountId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC123456789"));
        }

        @Test
        @DisplayName("should return 404 for non-existent account")
        void getAccount_nonExistentId_returns404() throws Exception {
            // Given
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            when(queryService.getAccountById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Account not found"));

            // When & Then
            mockMvc.perform(get("/api/v1/accounts/" + nonExistentId))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts")
    class GetAllAccountsTests {

        @Test
        @DisplayName("should return empty list when no accounts")
        void getAllAccounts_empty_returnsEmptyList() throws Exception {
            // Given
            when(queryService.getAllAccounts()).thenReturn(java.util.Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }
    }
}
