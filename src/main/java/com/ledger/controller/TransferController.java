package com.ledger.controller;

import com.ledger.dto.TransferRequest;
import com.ledger.service.AccountCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Money Transfer operations.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final AccountCommandService commandService;

    @PostMapping
    public ResponseEntity<TransferResponse> transferMoney(@Valid @RequestBody TransferRequest request) {
        commandService.transferMoney(request);
        return ResponseEntity.ok(TransferResponse.builder()
            .transferId(UUID.randomUUID())
            .fromAccountId(request.getFromAccountId())
            .toAccountId(request.getToAccountId())
            .amount(request.getAmount())
            .status("COMPLETED")
            .message("Transfer completed successfully")
            .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransferResponse {
        private UUID transferId;
        private UUID fromAccountId;
        private UUID toAccountId;
        private java.math.BigDecimal amount;
        private String status;
        private String message;
    }
}
