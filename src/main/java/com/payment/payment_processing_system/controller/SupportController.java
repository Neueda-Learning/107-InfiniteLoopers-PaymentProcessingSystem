package com.payment.payment_processing_system.controller;

import com.payment.payment_processing_system.dto.SupportDashboardResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for support team operations.
 * Exposes endpoints for dashboard analytics and transaction management.
 */
@Slf4j
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    /**
     * GET /api/support/dashboard
     * Retrieve system-wide dashboard statistics.
     *
     * @return 200 OK with SupportDashboardResponse containing system metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<SupportDashboardResponse> getDashboard() {
        log.info("GET /api/support/dashboard - Fetching dashboard statistics");
        SupportDashboardResponse dashboard = supportService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/support/transactions
     * Retrieve all payment transactions in the system, sorted newest first.
     *
     * @return 200 OK with list of all TransactionResponse objects
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("GET /api/support/transactions - Fetching all transactions");
        List<TransactionResponse> transactions = supportService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/support/customer/{accountNumber}
     * Retrieve all transactions (sent and received) for a specific customer account.
     *
     * @param accountNumber the account number to retrieve transactions for
     * @return 200 OK with list of TransactionResponse objects for the account
     */
    @GetMapping("/customer/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCustomer(
            @PathVariable String accountNumber) {
        log.info("GET /api/support/customer/{} - Fetching transactions for account", accountNumber);
        List<TransactionResponse> transactions = supportService.getTransactionsByCustomer(accountNumber);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/support/status/{status}
     * Retrieve all transactions filtered by a specific PaymentStatus.
     *
     * @param status the PaymentStatus enum value to filter by (e.g. CREATED, COMPLETED, FAILED)
     * @return 200 OK with list of matching TransactionResponse objects
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByStatus(
            @PathVariable PaymentStatus status) {
        log.info("GET /api/support/status/{} - Fetching transactions by status", status);
        List<TransactionResponse> transactions = supportService.getTransactionsByStatus(status);
        return ResponseEntity.ok(transactions);
    }
}

