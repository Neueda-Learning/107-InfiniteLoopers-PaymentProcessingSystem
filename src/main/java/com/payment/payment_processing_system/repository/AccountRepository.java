package com.payment.payment_processing_system.repository;

import com.payment.payment_processing_system.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Account entity.
 * Provides database operations for Account records.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find an account by account number.
     *
     * @param accountNumber the account number to search for
     * @return an Optional containing the Account if found, empty otherwise
     */
    Optional<Account> findByAccountNumber(String accountNumber);
}

