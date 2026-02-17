package com.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Transfer REST Controller
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class TransferControllerIntegrationTest {

    @Test
    void shouldTransferMoneyViaRestApi() {
        // TODO: Implement in Phase 3
    }

    @Test
    void shouldRejectInvalidTransferViaRestApi() {
        // TODO: Implement in Phase 3
    }
}
