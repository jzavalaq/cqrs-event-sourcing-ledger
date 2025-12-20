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
public class TransactionResponse {

    private UUID id;
    private UUID accountId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private UUID relatedAccountId;
    private Instant createdAt;
}
