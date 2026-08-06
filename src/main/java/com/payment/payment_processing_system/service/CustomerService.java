package com.payment.payment_processing_system.service;

import com.payment.payment_processing_system.dto.CustomerAccountResponse;
import com.payment.payment_processing_system.dto.CustomerListItemResponse;
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
    List<CustomerListItemResponse> getAllCustomers();

    /**
     * Retrieve all active accounts for a specific customer.
     *
     * @param customerId the ID of the customer
     * @return a List of active accounts for the customer
     */
    List<CustomerAccountResponse> getActiveAccountsByCustomerId(Long customerId);

    /**
     * Retrieve all active accounts by a customer identifier.
     * At least one identifier must be provided.
     * If multiple identifiers are provided, priority is: email > phone > name
     *
     * @param customerName customer name (optional)
     * @param email customer email (optional)
     * @param phoneNumber customer phone number (optional)
     * @return a List of active accounts for the matched customer
     * @throws IllegalArgumentException if no identifiers provided or if name matches are ambiguous
     * @throws CustomerNotFoundException if customer not found with provided identifier
     */
    List<CustomerAccountResponse> getActiveAccountsByCustomerIdentifier(String customerName, String email, String phoneNumber);

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

