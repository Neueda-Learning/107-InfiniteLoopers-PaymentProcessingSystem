package com.payment.payment_processing_system.service;

import com.payment.payment_processing_system.dto.SupportDashboardResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.PaymentStatus;

import java.util.List;

/**
 * Service interface for support team operations and dashboard analytics.
 */
public interface SupportService {

    /**
     * Retrieve the dashboard with system statistics and metrics.
     *
     * @return the SupportDashboardResponse containing system analytics
     */
    SupportDashboardResponse getDashboard();

    /**
     * Retrieve all payment transactions in the system.
     *
     * @return a List of all TransactionResponse objects
     */
    List<TransactionResponse> getAllTransactions();

    /**
     * Retrieve all transactions for a specific customer account.
     *
     * @param accountNumber the account number to retrieve transactions for
     * @return a List of TransactionResponse objects for the account
     */
    List<TransactionResponse> getTransactionsByCustomer(String accountNumber);

    /**
     * Retrieve all transactions with a specific payment status.
     *
     * @param paymentStatus the PaymentStatus filter
     * @return a List of TransactionResponse objects with the specified status
     */
    List<TransactionResponse> getTransactionsByStatus(PaymentStatus paymentStatus);

    /**
     * Retrieve all FAILED transactions ordered by failed time descending.
     * Used for the audit trail showing failure reasons.
     *
     * @return a List of TransactionResponse objects with FAILED status and failure reasons
     */
    List<TransactionResponse> getAuditTrail();
}

