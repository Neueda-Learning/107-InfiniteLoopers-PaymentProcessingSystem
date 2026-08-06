package com.payment.payment_processing_system.controller;

import com.payment.payment_processing_system.dto.CustomerAccountResponse;
import com.payment.payment_processing_system.dto.CustomerListItemResponse;
import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import jakarta.validation.constraints.Positive;
import com.payment.payment_processing_system.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * REST controller for customer-related operations.
 * Exposes endpoints for retrieving customer and transaction information.
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService customerService;

    /**
     * GET /api/customers
     * Retrieve all customers in the system.
     *
     * @return 200 OK with list of all CustomerResponse objects
     */
    @GetMapping
    public ResponseEntity<List<CustomerListItemResponse>> getAllCustomers() {
        log.info("GET /api/customers - Fetching all customers");
        List<CustomerListItemResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * GET /api/customers/{customerId}/accounts
     * Retrieve all active accounts for a customer.
     *
     * @param customerId the ID of the customer
     * @return 200 OK with active account list
     */
    @GetMapping("/{customerId}/accounts")
    public ResponseEntity<List<CustomerAccountResponse>> getCustomerAccounts(
            @PathVariable @Positive(message = "customerId must be a positive number") Long customerId) {
        log.info("GET /api/customers/{}/accounts - Fetching active accounts", customerId);
        List<CustomerAccountResponse> accounts = customerService.getActiveAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * GET /api/customers/accounts?customerName=... | ?email=... | ?phoneNumber=...
     * Retrieve all active accounts for a customer by identifier.
     * Exactly one query parameter should be provided.
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<CustomerAccountResponse>> getCustomerAccountsByIdentifier(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber) {
        log.info("GET /api/customers/accounts - Fetching active accounts by identifier");
        List<CustomerAccountResponse> accounts = customerService
                .getActiveAccountsByCustomerIdentifier(customerName, email, phoneNumber);
        return ResponseEntity.ok(accounts);
    }

    /**
     * GET /api/customers/{customerId}
     * Retrieve a customer by their unique customer ID.
     *
     * @param customerId the ID of the customer
     * @return 200 OK with the matching CustomerResponse
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable @Positive(message = "customerId must be a positive number") Long customerId) {
        log.info("GET /api/customers/{} - Fetching customer by ID", customerId);
        CustomerResponse customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(customer);
    }

    /**
     * GET /api/customers/account/{accountNumber}
     * Retrieve a customer by their associated account number.
     *
     * @param accountNumber the account number linked to the customer
     * @return 200 OK with the matching CustomerResponse
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<CustomerResponse> getCustomerByAccountNumber(
            @PathVariable String accountNumber) {
        log.info("GET /api/customers/account/{} - Fetching customer by account number", accountNumber);
        CustomerResponse customer = customerService.getCustomerByAccountNumber(accountNumber);
        return ResponseEntity.ok(customer);
    }

    /**
     * GET /api/customers/{accountNumber}/transactions
     * Retrieve the full transaction history for a customer account.
     *
     * @param accountNumber the account number to retrieve history for
     * @return 200 OK with list of TransactionResponse objects sorted newest first
     */
    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable String accountNumber) {
        log.info("GET /api/customers/{}/transactions - Fetching transaction history", accountNumber);
        List<TransactionResponse> transactions = customerService.getTransactionHistory(accountNumber);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/customers/transaction/{transactionId}
     * Retrieve details of a specific transaction by transaction ID.
     *
     * @param transactionId the unique transaction ID
     * @return 200 OK with the matching TransactionResponse
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionDetails(
            @PathVariable String transactionId) {
        log.info("GET /api/customers/transaction/{} - Fetching transaction details", transactionId);
        TransactionResponse transaction = customerService.getTransactionDetails(transactionId);
        return ResponseEntity.ok(transaction);
    }
}

