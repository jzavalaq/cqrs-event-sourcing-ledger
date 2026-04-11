package com.ledger.repository;

import com.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for LedgerEntry entities.
 * <p>
 * Provides access to transaction history and ledger entries for accounts.
 * </p>
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /**
     * Finds all ledger entries for an account, ordered by creation date (newest first).
     *
     * @param accountId the account ID
     * @return list of ledger entries
     */
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Finds ledger entries for an account with pagination.
     *
     * @param accountId the account ID
     * @param pageable pagination parameters
     * @return paginated ledger entries
     */
    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    /**
     * Counts the number of ledger entries for an account.
     *
     * @param accountId the account ID
     * @return the count of entries
     */
    long countByAccountId(UUID accountId);
}
