package com.payment.payment_processing_system.service;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PaymentResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;

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

