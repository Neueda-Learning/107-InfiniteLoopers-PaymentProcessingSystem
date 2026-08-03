package com.payment.payment_processing_system.controller;

import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for customer-related operations.
 * Exposes endpoints for retrieving customer and transaction information.
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * GET /api/customers
     * Retrieve all customers in the system.
     *
     * @return 200 OK with list of all CustomerResponse objects
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        log.info("GET /api/customers - Fetching all customers");
        List<CustomerResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * GET /api/customers/{customerId}
     * Retrieve a customer by their unique customer ID.
     *
     * @param customerId the ID of the customer
     * @return 200 OK with the matching CustomerResponse
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long customerId) {
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

