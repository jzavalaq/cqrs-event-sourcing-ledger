package com.ledger;

import com.ledger.config.TestSecurityConfig;
import com.ledger.dto.TransferRequest;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.exception.InvalidOperationException;
import com.ledger.exception.ResourceNotFoundException;
import com.ledger.service.AccountCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Transfer REST Controller.
 * Uses SpringBootTest with AutoConfigureMockMvc for full context testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountCommandService commandService;

    @Nested
    @DisplayName("POST /api/v1/transfers")
    class TransferMoneyTests {

        @Test
        @DisplayName("should transfer money successfully - returns 200")
        void transferMoney_validRequest_returns200() throws Exception {
            // Given
            UUID sourceAccountId = UUID.randomUUID();
            UUID destAccountId = UUID.randomUUID();

            TransferRequest request = TransferRequest.builder()
                .fromAccountId(sourceAccountId)
                .toAccountId(destAccountId)
                .amount(new BigDecimal("300.00"))
                .description("Payment for services")
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should return 400 for insufficient funds")
        void transferMoney_insufficientFunds_returns400() throws Exception {
            // Given
            UUID sourceAccountId = UUID.randomUUID();
            UUID destAccountId = UUID.randomUUID();

            TransferRequest request = TransferRequest.builder()
                .fromAccountId(sourceAccountId)
                .toAccountId(destAccountId)
                .amount(new BigDecimal("5000.00"))
                .description("Large transfer")
                .build();

            Mockito.doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(commandService).transferMoney(any());

            // When & Then
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for same account transfer")
        void transferMoney_sameAccount_returns400() throws Exception {
            // Given
            UUID accountId = UUID.randomUUID();

            TransferRequest request = TransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(accountId)
                .amount(new BigDecimal("100.00"))
                .description("Self transfer")
                .build();

            Mockito.doThrow(new InvalidOperationException("Cannot transfer to same account"))
                .when(commandService).transferMoney(any());

            // When & Then
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 for non-existent source account")
        void transferMoney_nonExistentSource_returns404() throws Exception {
            // Given
            UUID sourceAccountId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            UUID destAccountId = UUID.randomUUID();

            TransferRequest request = TransferRequest.builder()
                .fromAccountId(sourceAccountId)
                .toAccountId(destAccountId)
                .amount(new BigDecimal("100.00"))
                .description("Transfer")
                .build();

            Mockito.doThrow(new ResourceNotFoundException("Account not found"))
                .when(commandService).transferMoney(any());

            // When & Then
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 for missing description")
        void transferMoney_missingDescription_returns400() throws Exception {
            // Given
            UUID sourceAccountId = UUID.randomUUID();
            UUID destAccountId = UUID.randomUUID();

            String requestBody = String.format("""
                {
                    "fromAccountId": "%s",
                    "toAccountId": "%s",
                    "amount": 100.00
                }
                """, sourceAccountId, destAccountId);

            // When & Then
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }
}
