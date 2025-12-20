package com.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private UUID id;
    private String accountNumber;
    private String accountHolder;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Integer transactionCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
