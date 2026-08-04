package com.payment.payment_processing_system.repository;

import com.payment.payment_processing_system.model.TransactionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for TransactionStatusHistory entity.
 * Provides database operations for TransactionStatusHistory records.
 */
@Repository
public interface TransactionStatusHistoryRepository extends JpaRepository<TransactionStatusHistory, Long> {

    /**
     * Find all status history records for a specific transaction.
     *
     * @param transactionId the transaction ID to search for
     * @return a List of TransactionStatusHistory records for the given transaction
     */
    List<TransactionStatusHistory> findByTransactionTransactionId(String transactionId);
}

