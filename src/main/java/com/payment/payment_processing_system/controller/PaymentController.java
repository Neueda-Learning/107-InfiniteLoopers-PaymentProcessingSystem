package com.payment.payment_processing_system.controller;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PaymentResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for payment transaction operations.
 * Exposes endpoints for initiating, retrying, and retrieving payment transactions.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/send
     * Initiate a new payment transaction.
     *
     * @param paymentRequest the payment details including sender, receiver, amount and UPI PIN
     * @return 201 CREATED with PaymentResponse containing transaction confirmation
     */
    @PostMapping("/send")
    public ResponseEntity<PaymentResponse> sendMoney(@RequestBody PaymentRequest paymentRequest) {
        log.info("POST /api/payments/send - Initiating payment from account: {}",
                paymentRequest.getSenderAccountNumber());
        PaymentResponse response = paymentService.sendMoney(paymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/payments/retry/{transactionId}
     * Retry a previously failed payment transaction.
     *
     * @param transactionId the ID of the failed transaction to retry
     * @return 200 OK with PaymentResponse containing the retry transaction confirmation
     */
    @PostMapping("/retry/{transactionId}")
    public ResponseEntity<PaymentResponse> retryPayment(@PathVariable String transactionId) {
        log.info("POST /api/payments/retry/{} - Retrying failed payment", transactionId);
        PaymentResponse response = paymentService.retryPayment(transactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payments/{transactionId}
     * Retrieve details of a specific payment transaction.
     *
     * @param transactionId the unique transaction ID
     * @return 200 OK with the matching TransactionResponse
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        log.info("GET /api/payments/{} - Fetching transaction details", transactionId);
        TransactionResponse transaction = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * GET /api/payments
     * Retrieve all payment transactions in the system.
     *
     * @return 200 OK with list of all TransactionResponse objects
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("GET /api/payments - Fetching all payment transactions");
        List<TransactionResponse> transactions = paymentService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }
}

