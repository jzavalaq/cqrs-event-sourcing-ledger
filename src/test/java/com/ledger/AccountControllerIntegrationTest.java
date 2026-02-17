package com.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Account REST Controller
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class AccountControllerIntegrationTest {

    @Test
    void shouldCreateAccountViaRestApi() {
        // TODO: Implement in Phase 3
    }

    @Test
    void shouldGetAccountViaRestApi() {
        // TODO: Implement in Phase 3
    }

    @Test
    void shouldCreditAccountViaRestApi() {
        // TODO: Implement in Phase 3
    }

    @Test
    void shouldDebitAccountViaRestApi() {
        // TODO: Implement in Phase 3
    }

    @Test
    void shouldReturn404ForNonExistentAccount() {
        // TODO: Implement in Phase 3
    }

    @Test
    void shouldReturn400ForInvalidInput() {
        // TODO: Implement in Phase 3
    }
}
