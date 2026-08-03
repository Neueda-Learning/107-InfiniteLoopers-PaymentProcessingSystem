package com.payment.payment_processing_system.repository;

import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentTransaction entity.
 * Provides database operations for PaymentTransaction records.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find a payment transaction by transaction ID.
     *
     * @param transactionId the unique transaction ID to search for
     * @return an Optional containing the PaymentTransaction if found, empty otherwise
     */
    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    /**
     * Find a payment transaction by idempotency key.
     * Used to detect and prevent duplicate transactions.
     *
     * @param idempotencyKey the idempotency key to search for
     * @return an Optional containing the PaymentTransaction if found, empty otherwise
     */
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all payment transactions sent from a specific account.
     *
     * @param accountNumber the account number of the sender
     * @return a List of PaymentTransaction records sent from the given account
     */
    List<PaymentTransaction> findBySenderAccountAccountNumber(String accountNumber);

    /**
     * Find all payment transactions received by a specific account.
     *
     * @param accountNumber the account number of the receiver
     * @return a List of PaymentTransaction records received by the given account
     */
    List<PaymentTransaction> findByReceiverAccountAccountNumber(String accountNumber);

    /**
     * Find all payment transactions by payment status.
     *
     * @param paymentStatus the status to filter by
     * @return a List of matching PaymentTransaction records
     */
    List<PaymentTransaction> findByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Count transactions by payment status.
     *
     * @param paymentStatus the status to count
     * @return the count of matching records
     */
    long countByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Sum the amount of all completed (credited) transactions for a receiver account.
     *
     * @param accountNumber the receiver account number
     * @param paymentStatus the status to filter by
     * @return the total credited amount, or null if none
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
           "WHERE t.receiverAccount.accountNumber = :accountNumber " +
           "AND t.paymentStatus = :paymentStatus")
    BigDecimal sumAmountByReceiverAccountAndStatus(String accountNumber, PaymentStatus paymentStatus);

    /**
     * Sum the amount of all completed (debited) transactions for a sender account.
     *
     * @param accountNumber the sender account number
     * @param paymentStatus the status to filter by
     * @return the total debited amount, or null if none
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
           "WHERE t.senderAccount.accountNumber = :accountNumber " +
           "AND t.paymentStatus = :paymentStatus")
    BigDecimal sumAmountBySenderAccountAndStatus(String accountNumber, PaymentStatus paymentStatus);

    /**
     * Sum the total amount of all completed transactions (system-wide credits).
     *
     * @param paymentStatus the status to filter by
     * @return the total amount
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.paymentStatus = :paymentStatus")
    BigDecimal sumAmountByStatus(PaymentStatus paymentStatus);
}

