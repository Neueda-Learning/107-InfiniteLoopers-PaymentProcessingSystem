package com.payment.payment_processing_system.service;

import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;

import java.util.List;

/**
 * Service interface for managing customer-related operations.
 */
public interface CustomerService {

    /**
     * Retrieve all customers in the system.
     *
     * @return a List of all CustomerResponse objects
     */
    List<CustomerResponse> getAllCustomers();

    /**
     * Retrieve a customer by their customer ID.
     *
     * @param customerId the ID of the customer
     * @return the CustomerResponse for the specified customer
     */
    CustomerResponse getCustomerById(Long customerId);

    /**
     * Retrieve a customer by their account number.
     *
     * @param accountNumber the account number to search for
     * @return the CustomerResponse associated with the account
     */
    CustomerResponse getCustomerByAccountNumber(String accountNumber);

    /**
     * Retrieve the transaction history for a specific account.
     *
     * @param accountNumber the account number to retrieve history for
     * @return a List of TransactionResponse objects for the account
     */
    List<TransactionResponse> getTransactionHistory(String accountNumber);

    /**
     * Retrieve detailed information about a specific transaction.
     *
     * @param transactionId the ID of the transaction
     * @return the TransactionResponse with full transaction details
     */
    TransactionResponse getTransactionDetails(String transactionId);
}

