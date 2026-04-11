package com.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the CQRS Event Sourcing Banking Ledger.
 * <p>
 * This application implements a banking ledger using the CQRS (Command Query Responsibility Segregation)
 * pattern with Event Sourcing. It provides REST APIs for account management, ledger entries,
 * and money transfers.
 * </p>
 *
 * @see com.ledger.controller.AccountController
 * @see com.ledger.controller.TransferController
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
public class LedgerApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
