package com.ledger.repository;

import com.ledger.domain.AccountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountProjectionRepository extends JpaRepository<AccountProjection, UUID> {

    Optional<AccountProjection> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
