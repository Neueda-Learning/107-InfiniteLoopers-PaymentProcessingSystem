package com.payment.payment_processing_system.service;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PaymentResponse;
import com.payment.payment_processing_system.dto.TransactionStatusHistoryResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.PaymentStatus;

import java.util.List;

/**
 * Service interface for managing payment transactions.
 */
public interface PaymentService {

    /**
     * Send money from one account to another.
     *
     * @param paymentRequest the payment request containing transaction details
     * @return the PaymentResponse with transaction confirmation
     */
    PaymentResponse sendMoney(PaymentRequest paymentRequest);

    /**
     * Send money from one account to another using an optional client-supplied
     * idempotency key for safe request retries.
     *
     * @param paymentRequest the payment request containing transaction details
     * @param idempotencyKey optional idempotency key from the client
     * @return the PaymentResponse with transaction confirmation or an idempotent replay result
     */
    PaymentResponse sendMoney(PaymentRequest paymentRequest, String idempotencyKey);

    /**
     * Retry a failed payment transaction.
     *
     * @param transactionId the ID of the transaction to retry
     * @return the PaymentResponse with retry confirmation
     */
    PaymentResponse retryPayment(String transactionId);

    /**
     * Retrieve details of a specific transaction.
     *
     * @param transactionId the ID of the transaction
     * @return the TransactionResponse with full transaction details
     */
    TransactionResponse getTransaction(String transactionId);

    /**
     * Retrieve all payment transactions in the system.
     *
     * @return a List of all TransactionResponse objects
     */
    List<TransactionResponse> getAllTransactions();

    /**
     * Retrieve all payment transactions filtered by status.
     *
     * @param paymentStatus the status filter
     * @return a List of TransactionResponse objects matching the status
     */
    List<TransactionResponse> getTransactionsByStatus(PaymentStatus paymentStatus);

    /**
     * Retrieve status transition history for a specific transaction.
     *
     * @param transactionId the transaction ID
     * @return chronological status history entries for the transaction
     */
    List<TransactionStatusHistoryResponse> getTransactionHistory(String transactionId);

    /**
     * Generate a unique transaction ID.
     *
     * @return a generated transaction ID string
     */
    String generateTransactionId();

    /**
     * Generate a unique idempotency key for duplicate prevention.
     *
     * @return a generated idempotency key string
     */
    String generateIdempotencyKey();
}

