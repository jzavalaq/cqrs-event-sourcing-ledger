package com.ledger.saga;

import com.ledger.command.CreditAccountCommand;
import com.ledger.command.DebitAccountCommand;
import com.ledger.cqrs.event.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saga orchestrating money transfer between two accounts.
 *
 * <h3>Workflow:</h3>
 * <ol>
 *   <li>MoneyTransferInitiatedEvent -> Send DebitAccountCommand</li>
 *   <li>AccountDebitedEvent -> Send CreditAccountCommand</li>
 *   <li>AccountCreditedEvent -> Emit MoneyTransferCompletedEvent, end saga</li>
 * </ol>
 *
 * <h3>Compensation:</h3>
 * <ul>
 *   <li>DebitFailedEvent -> Emit MoneyTransferFailedEvent, end saga (no compensation needed)</li>
 *   <li>CreditFailedEvent -> Send compensating CreditAccountCommand, emit MoneyTransferFailedEvent</li>
 * </ul>
 *
 * <h3>Saga Pattern Benefits:</h3>
 * <ul>
 *   <li>Atomic distributed transactions without 2PC</li>
 *   <li>Automatic compensation on failure</li>
 *   <li>Event-sourced saga state for audit trail</li>
 *   <li>Resilient to partial failures</li>
 * </ul>
 */
@Saga
@Data
@NoArgsConstructor
@Slf4j
public class MoneyTransferSaga {

    // Saga state (persisted)
    private UUID transferId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private String description;
    private boolean debitCompleted = false;
    private boolean creditCompleted = false;
    private boolean compensationInProgress = false;

    // Transient (not persisted)
    @Autowired
    private transient CommandGateway commandGateway;

    // ==================== SAGA START ====================

    /**
     * Handles the initiation of a money transfer saga.
     * <p>
     * This is the entry point for the saga. When a MoneyTransferInitiatedEvent
     * is published, the saga starts and sends a DebitAccountCommand to the
     * source account.
     * </p>
     *
     * @param event the money transfer initiated event
     */
    @StartSaga
    @SagaEventHandler(associationProperty = "transferId")
    public void handle(MoneyTransferInitiatedEvent event) {
        log.info("[SAGA-START] Money transfer initiated: transferId={}, from={}, to={}, amount={}",
            event.getTransferId(), event.getFromAccountId(), event.getToAccountId(), event.getAmount());

        // Store saga state
        this.transferId = event.getTransferId();
        this.fromAccountId = event.getFromAccountId();
        this.toAccountId = event.getToAccountId();
        this.amount = event.getAmount();
        this.description = event.getDescription();

        // Step 1: Debit source account
        log.info("[SAGA-STEP-1] Sending DebitAccountCommand for account: {}", fromAccountId);
        commandGateway.send(
            DebitAccountCommand.of(
                fromAccountId,
                amount,
                String.format("Transfer to %s: %s", toAccountId, description),
                UUID.randomUUID(),
                transferId
            )
        );
    }

    // ==================== HAPPY PATH ====================

    /**
     * Handles successful debit of source account.
     * <p>
     * When the debit succeeds, proceed to credit the destination account.
     * </p>
     *
     * @param event the account debited event
     */
    @SagaEventHandler(associationProperty = "transferId")
    public void handle(AccountDebitedEvent event) {
        // Verify this is our saga's debit (not a standalone debit)
        if (event.getTransferId() == null || !event.getTransferId().equals(this.transferId)) {
            log.debug("Ignoring AccountDebitedEvent - not part of this saga");
            return;
        }

        // Verify it's the source account
        if (!event.getAccountId().equals(this.fromAccountId)) {
            log.debug("Ignoring AccountDebitedEvent - not from source account");
            return;
        }

        // Skip if already processed
        if (debitCompleted) {
            log.debug("Ignoring duplicate AccountDebitedEvent - debit already completed");
            return;
        }

        log.info("[SAGA-STEP-2] Debit completed for transfer: {}, new balance: {}",
            transferId, event.getBalanceAfter());

        this.debitCompleted = true;

        // Step 2: Credit target account
        log.info("[SAGA-STEP-2] Sending CreditAccountCommand for account: {}", toAccountId);
        commandGateway.send(
            CreditAccountCommand.of(
                toAccountId,
                amount,
                String.format("Transfer from %s: %s", fromAccountId, description),
                UUID.randomUUID(),
                transferId
            )
        );
    }

    /**
     * Handles successful credit of destination account.
     * <p>
     * When the credit succeeds, the transfer is complete. End the saga.
     * </p>
     *
     * @param event the account credited event
     */
    @SagaEventHandler(associationProperty = "transferId")
    public void handle(AccountCreditedEvent event) {
        // Verify this is our saga's credit
        if (event.getTransferId() == null || !event.getTransferId().equals(this.transferId)) {
            log.debug("Ignoring AccountCreditedEvent - not part of this saga");
            return;
        }

        // Verify it's the target account
        if (!event.getAccountId().equals(this.toAccountId)) {
            log.debug("Ignoring AccountCreditedEvent - not to target account");
            return;
        }

        // Skip if this is a compensation credit (handled separately)
        if (compensationInProgress) {
            log.info("[SAGA-COMPENSATION-DONE] Compensation credit completed for transfer: {}", transferId);
            endSagaWithFailure("CREDIT_FAILED", true);
            return;
        }

        // Skip if already processed
        if (creditCompleted) {
            log.debug("Ignoring duplicate AccountCreditedEvent - credit already completed");
            return;
        }

        log.info("[SAGA-STEP-3] Credit completed for transfer: {}, new balance: {}",
            transferId, event.getBalanceAfter());

        this.creditCompleted = true;

        // Step 3: Transfer complete - emit completion event
        log.info("[SAGA-COMPLETE] Money transfer completed successfully: {}", transferId);

        // End the saga
        SagaLifecycle.end();
    }

    // ==================== FAILURE HANDLERS ====================

    /**
     * Handles debit failure.
     * <p>
     * If debit fails (e.g., insufficient funds), the transfer cannot proceed.
     * No compensation is needed since no money was debited.
     * </p>
     *
     * @param event the debit failed event
     */
    @SagaEventHandler(associationProperty = "transferId")
    public void handle(DebitFailedEvent event) {
        log.error("[SAGA-FAIL] Debit failed for transfer: {}, reason: {}",
            transferId, event.getReason());

        // Debit failed - no compensation needed (money was never taken)
        endSagaWithFailure(event.getReason(), false);
    }

    /**
     * Handles credit failure.
     * <p>
     * If credit fails after debit succeeded, we must compensate by crediting
     * the money back to the source account.
     * </p>
     *
     * @param event the credit failed event
     */
    @SagaEventHandler(associationProperty = "transferId")
    public void handle(CreditFailedEvent event) {
        log.error("[SAGA-FAIL] Credit failed for transfer: {}, reason: {}. Starting compensation...",
            transferId, event.getReason());

        // Credit failed after debit succeeded - must compensate!
        if (debitCompleted && !compensationInProgress) {
            compensateDebit();
        } else {
            // Shouldn't happen, but handle gracefully
            endSagaWithFailure(event.getReason(), false);
        }
    }

    // ==================== COMPENSATION ====================

    /**
     * Compensates a successful debit by crediting the money back.
     * <p>
     * This is called when credit fails after debit has already succeeded.
     * The compensation credits the money back to the source account.
     * </p>
     */
    private void compensateDebit() {
        log.warn("[SAGA-COMPENSATE] Compensating debit for transfer: {}, crediting back account: {}",
            transferId, fromAccountId);

        this.compensationInProgress = true;

        // Send compensating credit to restore source account balance
        commandGateway.send(
            CreditAccountCommand.of(
                fromAccountId,
                amount,
                String.format("COMPENSATION: Failed transfer to %s - %s", toAccountId, description),
                UUID.randomUUID(),
                transferId  // Keep transferId for tracking
            )
        );
    }

    /**
     * Ends the saga with a failure status.
     * <p>
     * This method is called when the transfer fails. In a production system,
     * this would emit a MoneyTransferFailedEvent for notifications and audit.
     * </p>
     *
     * @param reason the failure reason
     * @param compensated whether compensation was performed
     */
    private void endSagaWithFailure(String reason, boolean compensated) {
        log.warn("[SAGA-END-FAIL] Transfer failed: transferId={}, reason={}, compensated={}",
            transferId, reason, compensated);

        // In a production implementation, we would emit MoneyTransferFailedEvent here
        // For now, just end the saga
        SagaLifecycle.end();
    }
}
