package com.ledger.repository;

import com.ledger.domain.AccountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AccountProjection entities (Read Side).
 * <p>
 * Provides optimized read access to account data through projections.
 * </p>
 */
@Repository
public interface AccountProjectionRepository extends JpaRepository<AccountProjection, UUID> {

    /**
     * Finds an account projection by its account number.
     *
     * @param accountNumber the unique account number
     * @return Optional containing the projection if found
     */
    Optional<AccountProjection> findByAccountNumber(String accountNumber);

    /**
     * Checks if an account projection exists with the given account number.
     *
     * @param accountNumber the account number to check
     * @return true if a projection exists with this number
     */
    boolean existsByAccountNumber(String accountNumber);
}
