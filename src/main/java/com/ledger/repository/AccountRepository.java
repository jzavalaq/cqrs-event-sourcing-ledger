package com.ledger.repository;

import com.ledger.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Account entities (Write Side).
 * <p>
 * Provides CRUD operations and custom queries for the Account aggregate.
 * </p>
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Finds an account by its account number.
     *
     * @param accountNumber the unique account number
     * @return Optional containing the account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Checks if an account exists with the given account number.
     *
     * @param accountNumber the account number to check
     * @return true if an account exists with this number
     */
    boolean existsByAccountNumber(String accountNumber);
}
